package org.folio.mr.api;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import org.folio.mr.client.CirculationClient;
import org.folio.mr.client.SettingsClient;
import org.folio.mr.controller.delegate.BatchRequestsServiceDelegate;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserTenant;
import org.folio.mr.domain.dto.UserTenantsResponse;
import org.folio.spring.config.DataSourceFolioWrapper;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.test.types.IntegrationTest;

@IntegrationTest
@DatabaseCleanup(tables = { "batch_request_split", "batch_request", "mediated_request" })
@SpringBootTest(properties = {
  "folio.tenant.secure-tenant-id=secure",
  "folio.batch-requests.thread-pool-size=4",
  "spring.datasource.hikari.maximum-pool-size=2",
  "folio.batch-requests.print-result=false",
})
class MediatedBatchRequestsDbPoolUsageIT extends BaseIT {

  private static final int REPEAT_COUNT = 5;
  private static final int ITEMS_PER_BATCH_REQUEST = 25;
  private static final Duration MAX_AWAIT_TIMEOUT = Durations.ONE_MINUTE;
  private static final Duration AWAIT_POLL_INTERVAL = Durations.ONE_HUNDRED_MILLISECONDS;
  private static final Duration BATCH_REPEAT_REQUEST_CREATION_DELAY = Duration.ofMillis(100);

  private static final String TENANT_ID_SINGLE = "single";
  private static final String SERVICE_POINT_ID = "a0ab0704-2d16-4326-a6cd-9b6c15983bac";
  private static final String HOLDING_RECORD_ID = "7212fa7b-a06a-41ee-b908-50c8f1b37ebb";

  private static final List<String> TEST_TENANTS =
    List.of(TENANT_ID_SECURE, TENANT_ID_COLLEGE, TENANT_ID_CENTRAL, TENANT_ID_SINGLE, TENANT_ID_CONSORTIUM);

  @BeforeAll
  static void beforeAll(@Autowired DataSourceFolioWrapper datasource) {
    TEST_TENANTS.forEach(BaseIT::setUpTenant);
    var hikariCp = datasource.getTargetDataSource();
    assertThat(hikariCp, notNullValue());
    assertThat(hikariCp, isA(HikariDataSource.class));
    assertThat(((HikariDataSource) hikariCp).getMaximumPoolSize(), is(2));
  }

  @AfterAll
  static void afterAll() {
    TEST_TENANTS.forEach(BaseIT::tearDownTenant);
  }

  @Test
  void shouldCreateSingleTenantBatchRequestsWithoutDatabasePoolStarvation() {
    var tenants = List.of(TENANT_ID_COLLEGE, TENANT_ID_SINGLE, TENANT_ID_CENTRAL);
    tenants.forEach(MediatedBatchRequestsDbPoolUsageIT::setupWiremockForSingleTenantRequestCreation);

    var createdRequestsPerTenant = new LinkedHashMap<String, String>();
    for (var tenant : tenants) {
      var request = createBatchRequests(tenant, batchRequest());
      createdRequestsPerTenant.put(tenant, request.getBatchId());
    }

    createdRequestsPerTenant.forEach((tenant, batchId) ->
      awaitUntilAsserted(() -> verifyRequestCompletion(tenant, batchId)));
  }

  @Test
  void shouldCreateMixedSingleAndEcsBatchRequestsWithoutDatabasePoolStarvation() {
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_SINGLE);
    setupWiremockForEcsRequestCreation();

    var st1Request = createBatchRequests(TENANT_ID_SINGLE, batchRequest());
    var ecsRequest = createBatchRequests(TENANT_ID_CENTRAL, batchRequest());
    var st2Request = createBatchRequests(TENANT_ID_SINGLE, batchRequest());

    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_SINGLE, st1Request.getBatchId()));
    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_SINGLE, st2Request.getBatchId()));
    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_CENTRAL, ecsRequest.getBatchId()));
  }

  @Test
  void shouldCreateMixedSingleTenantAndSecureTenantBatchRequestsWithoutDbPoolStarvation() {
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_SINGLE);
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_COLLEGE);
    setupWiremockForSecureTenantRequestCreation();

    var st1Request = createBatchRequests(TENANT_ID_SINGLE, batchRequest());
    var secureRequest = createBatchRequests(TENANT_ID_SECURE, batchRequest());
    var st2Request = createBatchRequests(TENANT_ID_COLLEGE, batchRequest());

    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_SINGLE, st1Request.getBatchId()));
    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_SECURE, secureRequest.getBatchId()));
    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_COLLEGE, st2Request.getBatchId()));
  }

  @Test
  void shouldCreateMixedBatchRequestsWithoutDbPoolStarvation() {
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_SINGLE);
    setupWiremockForEcsRequestCreation();
    setupWiremockForSecureTenantRequestCreation();

    var stRequest = createBatchRequests(TENANT_ID_SINGLE, batchRequest());
    var ecsRequest = createBatchRequests(TENANT_ID_CENTRAL, batchRequest());
    var secureRequest = createBatchRequests(TENANT_ID_SECURE, batchRequest());

    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_SINGLE, stRequest.getBatchId()));
    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_SECURE, secureRequest.getBatchId()));
    awaitUntilAsserted(() -> verifyRequestCompletion(TENANT_ID_CENTRAL, ecsRequest.getBatchId()));
  }

  @Test
  void shouldExecuteSingleTenantRequestMultipleTimesWithoutStarvation() {
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_SINGLE);
    repeatBatchRequestCreation(TENANT_ID_SINGLE);
  }

  @Test
  void shouldExecuteEcsRequestMultipleTimesWithoutStarvation() {
    setupWiremockForEcsRequestCreation();
    repeatBatchRequestCreation(TENANT_ID_CENTRAL);
  }

  @Test
  void shouldExecuteSecureTenantRequestMultipleTimesWithoutStarvation() {
    setupWiremockForSecureTenantRequestCreation();
    repeatBatchRequestCreation(TENANT_ID_SECURE);
  }

  @SneakyThrows
  private MediatedBatchRequestDto createBatchRequests(String tenant, MediatedBatchRequestPostDto body) {
    mockMvc.perform(post("/requests-mediated/batch-mediated-requests")
        .headers(defaultHeaders(tenant))
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(body)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("batchId", is(body.getBatchId())))
      .andReturn().getResponse().getContentAsString();

    return OBJECT_MAPPER.readValue(asJsonString(body), MediatedBatchRequestDto.class);
  }

  private static MediatedBatchRequestPostDto batchRequest() {
    return new MediatedBatchRequestPostDto()
      .batchId(UUID.randomUUID().toString())
      .itemRequests(itemIdentifiers().stream()
        .map(UUID::toString)
        .map(MediatedBatchRequestsDbPoolUsageIT::itemRequestSplit)
        .toList())
      .requesterId(USER_ID)
      .mediatedWorkflow(MediatedBatchRequestPostDto.MediatedWorkflowEnum.MULTI_ITEM_REQUEST)
      .patronComments("Stability Testing comment");
  }

  private static MediatedBatchRequestPostDtoItemRequestsInner itemRequestSplit(String id) {
    return new MediatedBatchRequestPostDtoItemRequestsInner()
      .itemId(id)
      .pickupServicePointId(SERVICE_POINT_ID);
  }

  @SneakyThrows
  private void verifyRequestCompletion(String tenant, String batchId) {
    mockMvc.perform(get("/requests-mediated/batch-mediated-requests/{requestId}", batchId)
        .headers(defaultHeaders(tenant))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("mediatedRequestStatus", is("Completed")));
  }

  private static void awaitUntilAsserted(ThrowingRunnable throwingRunnable) {
    Awaitility.await()
      .pollInterval(AWAIT_POLL_INTERVAL)
      .atMost(MAX_AWAIT_TIMEOUT)
      .untilAsserted(throwingRunnable);
  }

  private static void setupWiremockForSingleTenantRequestCreation(String tenant) {
    mockHelper.mockGetSettingEntries(tenant, settingsQuery(), emptySettingsResponse());
    mockHelper.mockGetUserTenants(tenant, emptyUserTenants());
    mockHelper.mockGetAllowedServicePoints(tenant, allowedServicePointsResponse());
    mockHelper.mockGetInventoryHoldingRecord(tenant, HOLDING_RECORD_ID);
    mockHelper.mockGetInventoryItemAny(tenant, HOLDING_RECORD_ID);
    mockHelper.mockPostCirculationRequestAny(tenant);
  }

  private static void setupWiremockForEcsRequestCreation() {
    mockHelper.mockGetSettingEntries(TENANT_ID_CENTRAL, settingsQuery(), emptySettingsResponse());
    mockHelper.mockGetUserTenants(TENANT_ID_CENTRAL, userTenants(TENANT_ID_CENTRAL));
    mockHelper.mockGetConsortiumItemAny(TENANT_ID_CENTRAL, consortiumItem());
    mockHelper.mockPostEcsExternalRequestAny(TENANT_ID_CENTRAL);
    mockHelper.mockGetCirculationRequestByIdAny(TENANT_ID_COLLEGE, circulationRequest());
    mockHelper.mockGetCirculationRequestByIdAny(TENANT_ID_CENTRAL, circulationRequest());
  }

  private static void setupWiremockForSecureTenantRequestCreation() {
    mockHelper.mockGetSettingEntries(TENANT_ID_SECURE, settingsQuery(), emptySettingsResponse());
    mockHelper.mockGetUserTenants(TENANT_ID_SECURE, userTenants(TENANT_ID_SECURE));
    mockHelper.mockGetConsortiumItemAny(TENANT_ID_CENTRAL, consortiumItem());
    mockHelper.mockGetUserById(TENANT_ID_SECURE, activeUser());
    mockHelper.mockGetSearchInstancesEmpty(TENANT_ID_SECURE, emptySearchInstances());
  }

  private static UserTenantsResponse userTenants(String tenant) {
    return new UserTenantsResponse()
      .totalRecords(1)
      .userTenants(List.of(
        new UserTenant()
          .id(UUID.randomUUID())
          .userId(UUID.fromString(USER_ID))
          .username("consortia-system-user")
          .tenantId(tenant)
          .centralTenantId(TENANT_ID_CENTRAL)
          .consortiumId(UUID.randomUUID())
      ));
  }

  private static UserTenantsResponse emptyUserTenants() {
    return new UserTenantsResponse().totalRecords(0);
  }

  private static String settingsQuery() {
    return BatchRequestsServiceDelegate.BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY;
  }

  private static SearchInstancesResponse emptySearchInstances() {
    return new SearchInstancesResponse().totalRecords(0);
  }

  private static SettingsClient.SettingsEntries emptySettingsResponse() {
    return new SettingsClient.SettingsEntries(
      Collections.emptyList(),
      new SettingsClient.ResultInfo(0));
  }

  private static User activeUser() {
    return new User().id(USER_ID).active(true);
  }

  private static CirculationClient.AllowedServicePoints allowedServicePointsResponse() {
    return new CirculationClient.AllowedServicePoints(
      emptySet(), Set.of(new ServicePoint().id(SERVICE_POINT_ID)), emptySet());
  }

  private static ConsortiumItem consortiumItem() {
    return new ConsortiumItem()
      .id("{{request.path.id}}")
      .tenantId(TENANT_ID_COLLEGE)
      .instanceId(UUID.randomUUID().toString())
      .id(HOLDING_RECORD_ID);
  }

  private static Request circulationRequest() {
    return new Request()
      .id("{{request.path.id}}")
      .itemId(UUID.randomUUID().toString())
      .instanceId(UUID.randomUUID().toString())
      .holdingsRecordId(HOLDING_RECORD_ID)
      .requesterId(USER_ID)
      .pickupServicePointId(SERVICE_POINT_ID);
  }

  private static List<UUID> itemIdentifiers() {
    return Stream.generate(UUID::randomUUID).limit(ITEMS_PER_BATCH_REQUEST).toList();
  }

  private static void testDelay() {
    Awaitility.await()
      .pollDelay(BATCH_REPEAT_REQUEST_CREATION_DELAY)
      .untilAsserted(() -> assertThat(true, is(true)));
  }

  private void repeatBatchRequestCreation(String tenant) {
    var ids = new ArrayList<String>();
    for (int i = 0; i < REPEAT_COUNT; i++) {
      var request = createBatchRequests(tenant, batchRequest());
      ids.add(request.getBatchId());
      testDelay();
    }

    ids.forEach(id -> awaitUntilAsserted(() -> verifyRequestCompletion(tenant, id)));
  }
}
