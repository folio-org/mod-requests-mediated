package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.mr.api.BaseIT.TENANT_ID_SECURE;
import static org.folio.mr.controller.delegate.BatchRequestsServiceDelegate.BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY;
import static org.folio.mr.controller.delegate.BatchRequestsServiceDelegate.SETTING_KEY;
import static org.folio.mr.controller.delegate.BatchRequestsServiceDelegate.SETTING_SCOPE;
import static org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum.IN_PROGRESS;
import static org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum.PENDING;
import static org.folio.mr.domain.type.ErrorCode.BATCH_REQUEST_ITEM_IDS_COUNT_EXCEEDS_MAX_LIMIT;
import static org.folio.mr.domain.type.ErrorCode.DUPLICATE_BATCH_REQUEST_ID;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.folio.mr.client.SettingsClient;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Instance;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequests;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.dto.UserTenant;
import org.folio.mr.domain.dto.UserTenantsResponse;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.MediatedBatchRequestValidationException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.flow.BatchFailedFlowFinalizer;
import org.folio.mr.service.flow.BatchFlowFinalizer;
import org.folio.mr.service.flow.BatchFlowInitializer;
import org.folio.mr.service.flow.BatchSplitProcessor;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;


@IntegrationTest
@SpringBootTest(properties = {"folio.tenant.secure-tenant-id=" + TENANT_ID_SECURE})
class MediatedBatchRequestsControllerIT extends BaseIT {

  private static final String URL_MEDIATED_BATCH_REQUESTS = "/requests-mediated/batch-mediated-requests";
  private static final String URL_MEDIATED_REQUESTS = "/requests-mediated/mediated-requests";
  private static final String REQUESTER_ID = "9812e24b-0a66-457a-832c-c5e789797e35";
  private static final String BATCH_REQUEST_ID1 = UUID.randomUUID().toString();
  private static final String BATCH_REQUEST_ID2 = UUID.randomUUID().toString();
  private static final String[] ITEM_IDS = {"211c74f5-ebb1-42bf-b752-653fef171585", "45172d4e-0484-408d-868d-70ea269152ab"};
  private static final String INSTANCE_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
  private static final UUID[] SERVICE_POINT_IDS = {
    UUID.fromString("3afab82b-b4e5-4da6-9a3d-e6891770f0bb"),
    UUID.fromString("53c12e46-c1c2-4181-8671-cd044ca566f1")
  };
  private static final String SEARCH_ITEM_URL = "/search/consortium/item";
  private static final String ECS_TLR_EXTERNAL_URL = "/tlr/create-ecs-request-external";
  private static final String SETTINGS_ENTRIES_URL = "/settings/entries";
  private static final UUID[] EXPECTED_CREATED_REQUEST_IDS = {
    UUID.fromString("9a326225-6530-41cc-9399-a61987bfab3c"),
    UUID.fromString("16f40c4e-235d-4912-a683-2ad919cc8b07")};

  @Autowired
  private MediatedBatchRequestRepository batchRequestRepository;

  @Autowired
  private MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @MockitoSpyBean
  private BatchFlowInitializer flowInitializer;

  @MockitoSpyBean
  private BatchFlowFinalizer flowFinalizer;

  @MockitoSpyBean
  private BatchSplitProcessor batchSplitProcessor;

  @MockitoSpyBean
  private BatchFailedFlowFinalizer failedFlowFinalizer;

  @BeforeEach
  void clearDatabase() {
    batchRequestSplitRepository.deleteAll();
    batchRequestRepository.deleteAll();

    mockGetUserTenants(TENANT_ID_CONSORTIUM, TENANT_ID_CONSORTIUM);
  }

  // GET Batch Requests Collection tests

  @Test
  @SneakyThrows
  void getReturnsEmptyArrayWhenNoRequestsAreFound() {
    getAllRequests()
      .andExpect(status().isOk())
      .andExpect(jsonPath("mediatedBatchRequests", emptyIterable()))
      .andExpect(jsonPath("totalRecords", is(0)));
  }

  @MethodSource("cqlQueryProvider")
  @ParameterizedTest(name = "query = ({0})")
  @DisplayName("Get Collection of Mediated Batch Requests by CQL query")
  @SneakyThrows
  void shouldReturnBatchRequestsForGivenQuery(String cql, int total, List<String> ids) {
    var postDto1 = sampleBatchRequestPostDto(ITEM_IDS[0]).batchId(BATCH_REQUEST_ID1);
    var postDto2 = sampleBatchRequestPostDto(ITEM_IDS[0]).batchId(BATCH_REQUEST_ID2);
    addStubForSettings();

    createBatchRequests(postDto1)
      .andExpect(jsonPath("mediatedRequestStatus",
        oneOf(PENDING.getValue(), IN_PROGRESS.getValue())));
    createBatchRequests(postDto2)
      .andExpect(jsonPath("mediatedRequestStatus",
        oneOf(PENDING.getValue(), IN_PROGRESS.getValue())));

    var response = getRequestsByQuery(cql)
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(total)))
      .andReturn().getResponse().getContentAsString();
    var collectionDto = OBJECT_MAPPER.readValue(response, MediatedBatchRequestsDto.class);

    assertThat(collectionDto.getMediatedBatchRequests())
      .extracting(MediatedBatchRequestDto::getBatchId)
      .containsExactlyInAnyOrder(ids.toArray(new String[0]));
  }

  // GET Batch Request by ID tests

  @Test
  @SneakyThrows
  void shouldReturnBatchRequestById() {
    var createdRequest = createAndSaveBatchRequestEntity(BatchRequestStatus.IN_PROGRESS);
    createAndSaveBatchRequestSplitEntity(createdRequest, BatchRequestSplitStatus.IN_PROGRESS);
    createAndSaveBatchRequestSplitEntity(createdRequest, BatchRequestSplitStatus.COMPLETED);
    createAndSaveBatchRequestSplitEntity(createdRequest, BatchRequestSplitStatus.FAILED);
    createAndSaveBatchRequestSplitEntity(createdRequest, BatchRequestSplitStatus.PENDING);

    assertThat(batchRequestRepository.count()).isEqualTo(1);
    assertThat(batchRequestSplitRepository.count()).isEqualTo(4);

    mockMvc.perform(
        get(URL_MEDIATED_BATCH_REQUESTS + "/" + BATCH_REQUEST_ID1)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID1)))
      .andExpect(jsonPath("itemRequestsStats.total", is(4)))
      .andExpect(jsonPath("itemRequestsStats.inProgress", is(1)))
      .andExpect(jsonPath("itemRequestsStats.completed", is(1)))
      .andExpect(jsonPath("itemRequestsStats.pending", is(1)))
      .andExpect(jsonPath("itemRequestsStats.failed", is(1)));
  }

  @Test
  @SneakyThrows
  void shouldReturnNotFoundForGetById() {
    mockMvc.perform(
        get(URL_MEDIATED_BATCH_REQUESTS + "/" + UUID.randomUUID())
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(exceptionMatch(MediatedBatchRequestNotFoundException.class));
  }

  // POST Batch Request in ECS environment tests

  @Test
  @SneakyThrows
  void shouldCreateMediatedBatchRequest() {
    var postBatchRequestDto = sampleBatchRequestPostDto(ITEM_IDS);

    addStubForSettings();
    createBatchRequests(postBatchRequestDto)
      .andExpect(jsonPath("mediatedRequestStatus", oneOf(PENDING.getValue(), IN_PROGRESS.getValue())))
      .andExpect(jsonPath("requesterId", is(REQUESTER_ID)))
      .andExpect(jsonPath("patronComments", is(postBatchRequestDto.getPatronComments())))
      .andExpect(jsonPath("mediatedWorkflow", is(postBatchRequestDto.getMediatedWorkflow().getValue())))
      .andExpect(jsonPath("metadata.createdDate").exists())
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUsername").doesNotExist())
      .andExpect(jsonPath("metadata.updatedDate").exists())
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.updatedByUsername").doesNotExist());

    assertThat(batchRequestRepository.count()).isEqualTo(1L);
    assertThat(batchRequestSplitRepository.count()).isEqualTo(2L);
  }

  @Test
  @SneakyThrows
  void shouldCreateMediatedBatchRequestWithProvidedId() {
    var postBatchRequestDto = sampleBatchRequestPostDto(ITEM_IDS)
      .batchId(BATCH_REQUEST_ID1);

    addStubsForEcsRequestCreation(EXPECTED_CREATED_REQUEST_IDS, ITEM_IDS);

    createBatchRequests(postBatchRequestDto)
      .andExpect(jsonPath("mediatedRequestStatus",
        oneOf(PENDING.getValue(), IN_PROGRESS.getValue())))
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID1)));

    assertThat(batchRequestRepository.count()).isEqualTo(1L);
    assertThat(batchRequestSplitRepository.count()).isEqualTo(2L);

    var captor = ArgumentCaptor.forClass(BatchContext.class);
    Awaitility.await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE)
      .untilAsserted(() -> {
        verify(flowInitializer).execute(captor.capture());
        var batchContext = captor.getValue();
        assertEquals(BATCH_REQUEST_ID1, batchContext.getBatchRequestId().toString());
        assertThat(batchContext.getBatchSplitEntitiesById()).hasSize(2);

        verify(batchSplitProcessor, times(2)).execute(any(BatchSplitContext.class));
        verify(flowFinalizer).execute(any(BatchContext.class));

        assertThat(batchRequestSplitRepository.findAll())
          .extracting(
            MediatedBatchRequestSplit::getStatus,
            MediatedBatchRequestSplit::getConfirmedRequestId,
            MediatedBatchRequestSplit::getRequestStatus)
          .containsExactlyInAnyOrder(
            tuple(BatchRequestSplitStatus.COMPLETED, EXPECTED_CREATED_REQUEST_IDS[0], "Open - Not yet filled"),
            tuple(BatchRequestSplitStatus.COMPLETED, EXPECTED_CREATED_REQUEST_IDS[1], "Open - Not yet filled")
          );
        assertEquals(BatchRequestStatus.COMPLETED, batchRequestRepository.findAll().getFirst().getStatus());
      });
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(SEARCH_ITEM_URL + "/" + ITEM_IDS[0])));
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(SEARCH_ITEM_URL + "/" + ITEM_IDS[1])));
    wireMockServer.verify(2, postRequestedFor(urlPathEqualTo(ECS_TLR_EXTERNAL_URL)));
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(SETTINGS_ENTRIES_URL)));
  }

  @Test
  @SneakyThrows
  void shouldFailToCreateMediatedBatchRequestWithItemsLimitExceedError() {
    var postBatchRequestDto = sampleBatchRequestPostDto(ITEM_IDS)
      .batchId(BATCH_REQUEST_ID1);

    var settingsEntries = new SettingsClient.SettingsEntries(List.of(
      new SettingsClient.SettingEntry(UUID.randomUUID(), SETTING_SCOPE, SETTING_KEY,
        new SettingsClient.BatchRequestItemsValidationValue(1))
    ),
      new SettingsClient.ResultInfo(1));

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(SETTINGS_ENTRIES_URL))
      .withQueryParam("query", containing(BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY))
      .withQueryParam("limit", equalTo("1"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(settingsEntries, HttpStatus.SC_OK)));

    mockMvc.perform(post(URL_MEDIATED_BATCH_REQUESTS)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(postBatchRequestDto)))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is(BATCH_REQUEST_ITEM_IDS_COUNT_EXCEEDS_MAX_LIMIT.getMessage())))
      .andExpect(exceptionMatch(MediatedBatchRequestValidationException.class));
  }

  @Test
  @SneakyThrows
  void shouldHandleInitializerFlowErrorAndSetFailedStatuses() {
    var postBatchRequestDto = sampleBatchRequestPostDto(ITEM_IDS[0]).batchId(BATCH_REQUEST_ID1);

    addStubForSettings();
    doThrow(new MediatedBatchRequestNotFoundException(UUID.fromString(BATCH_REQUEST_ID1)))
      .when(flowInitializer).execute(any(BatchContext.class));

    createBatchRequests(postBatchRequestDto);

    assertThat(batchRequestRepository.count()).isEqualTo(1L);
    assertThat(batchRequestSplitRepository.count()).isEqualTo(1L);
    Awaitility.await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE)
      .untilAsserted(() -> {
        assertThat(batchRequestSplitRepository.findAll())
          .extracting(
            MediatedBatchRequestSplit::getStatus,
            MediatedBatchRequestSplit::getErrorDetails)
          .containsExactlyInAnyOrder(
            tuple(BatchRequestSplitStatus.FAILED, "Mediated Batch Request with ID [%s] was not found"
              .formatted(BATCH_REQUEST_ID1)));
        assertEquals(BatchRequestStatus.FAILED, batchRequestRepository.findAll().getFirst().getStatus());
      });
  }

  @Test
  @SneakyThrows
  void shouldHandleFinalizerFlowErrorAndSetFailedStatuses() {
    var postBatchRequestDto = sampleBatchRequestPostDto(ITEM_IDS).batchId(BATCH_REQUEST_ID1);
    var errorMessage = "Batch request with id %s has failed".formatted(BATCH_REQUEST_ID1);

    addStubsForEcsRequestCreation(EXPECTED_CREATED_REQUEST_IDS, ITEM_IDS);
    doThrow(new RuntimeException(errorMessage)).when(flowFinalizer).execute(any(BatchContext.class));

    createBatchRequests(postBatchRequestDto);

    assertThat(batchRequestRepository.count()).isEqualTo(1L);
    assertThat(batchRequestSplitRepository.count()).isEqualTo(2L);
    Awaitility.await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE)
      .untilAsserted(() -> {
        assertThat(batchRequestSplitRepository.findAll())
          .extracting(
            MediatedBatchRequestSplit::getStatus,
            MediatedBatchRequestSplit::getErrorDetails)
          .containsExactly(
            tuple(BatchRequestSplitStatus.COMPLETED, null),
            tuple(BatchRequestSplitStatus.COMPLETED, null));
        assertEquals(BatchRequestStatus.FAILED, batchRequestRepository.findAll().getFirst().getStatus());
      });
  }

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(strings = {"request placing failed", ""})
  void shouldHandleBatchSplitProcessingFailureAndMarkBatchSplitFailed(String errorMessage) {
    var postBatchRequestDto = sampleBatchRequestPostDto(ITEM_IDS[0])
      .batchId(BATCH_REQUEST_ID1);
    Mockito.doThrow(new RuntimeException(errorMessage))
      .when(batchSplitProcessor).execute(any(BatchSplitContext.class));

    var expectedCreatedRequestIds = new UUID[]{EXPECTED_CREATED_REQUEST_IDS[0]};
    var itemIds = new String[]{ITEM_IDS[0]};
    addStubsForEcsRequestCreation(expectedCreatedRequestIds, itemIds);

    createBatchRequests(postBatchRequestDto)
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID1)));

    var captor = ArgumentCaptor.forClass(BatchContext.class);
    Awaitility.await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE)
      .untilAsserted(() -> {
        verify(flowInitializer).execute(captor.capture());
        var batchContext = captor.getValue();
        assertEquals(BATCH_REQUEST_ID1, batchContext.getBatchRequestId().toString());
        assertThat(batchContext.getBatchSplitEntitiesById()).hasSize(1);

        verify(failedFlowFinalizer).execute(any(BatchContext.class));
      });

    var expectedErrorMsg = isNotBlank(errorMessage) ? errorMessage : "Failed to create request for item %s".formatted(ITEM_IDS[0]);
    assertThat(batchRequestSplitRepository.findAll())
      .extracting(MediatedBatchRequestSplit::getStatus, MediatedBatchRequestSplit::getConfirmedRequestId, MediatedBatchRequestSplit::getErrorDetails)
      .containsExactly(tuple(BatchRequestSplitStatus.FAILED, null, expectedErrorMsg));

    var batchRequest = batchRequestRepository.findById(UUID.fromString(BATCH_REQUEST_ID1)).orElseThrow();
    assertThat(batchRequest.getStatus()).isEqualTo(BatchRequestStatus.FAILED);
  }

  @Test
  @SneakyThrows
  void shouldThrowErrorOnCreateMediatedBatchRequest_BatchAlreadyExistsWithSameId() {
    var postBatchRequestDto1 = sampleBatchRequestPostDto(ITEM_IDS[0])
      .batchId(BATCH_REQUEST_ID1);
    var postBatchRequestDto2 = sampleBatchRequestPostDto(ITEM_IDS)
      .batchId(BATCH_REQUEST_ID1);
    addStubForSettings();

    createBatchRequests(postBatchRequestDto1);

    mockMvc.perform(post(URL_MEDIATED_BATCH_REQUESTS)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(postBatchRequestDto2)))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is(DUPLICATE_BATCH_REQUEST_ID.getMessage())))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));
  }

  // POST Batch Request in Secure Tenant environment tests

  @Test
  @SneakyThrows
  void shouldCreateMediatedBatchRequestWithProvidedIdInSecureTenantEnv() {
    setUpTenant(TENANT_ID_SECURE);

    var postBatchRequestDto = sampleBatchRequestPostDto(ITEM_IDS)
      .batchId(BATCH_REQUEST_ID1);

    addStubsForSecureTenantRequestCreationInEcs(ITEM_IDS);
    var headers = defaultHeaders();
    headers.put(XOkapiHeaders.TENANT, List.of(TENANT_ID_SECURE));

    mockMvc.perform(post(URL_MEDIATED_BATCH_REQUESTS)
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(postBatchRequestDto)))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("mediatedRequestStatus",
        oneOf(PENDING.getValue(), IN_PROGRESS.getValue())))
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID1)));

    assertThat(databaseHelper.countRowsWhere("batch_request", TENANT_ID_SECURE,
      "id='" + BATCH_REQUEST_ID1 + "'")).isEqualTo(1L);
    assertThat(databaseHelper.countRowsWhere("batch_request_split", TENANT_ID_SECURE,
      "batch_id='" + BATCH_REQUEST_ID1 + "'")).isEqualTo(2L);

    Awaitility.await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE)
      .untilAsserted(() -> {
        var response = mockMvc.perform(
            get(URL_MEDIATED_REQUESTS)
              .headers(headers)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("totalRecords", is(2)))
          .andExpect(jsonPath("mediatedRequests[*].status", is(List.of("New - Awaiting confirmation", "New - Awaiting confirmation"))))
          .andExpect(jsonPath("mediatedRequests[*].requestLevel", is(List.of("Item", "Item"))))
          .andExpect(jsonPath("mediatedRequests[*].instanceId", is(List.of(INSTANCE_ID, INSTANCE_ID))))
          .andExpect(jsonPath("mediatedRequests[*].itemId", containsInAnyOrder(ITEM_IDS[0], ITEM_IDS[1])))
          .andReturn().getResponse().getContentAsString();
        var mediatedRequests = OBJECT_MAPPER.readValue(response, MediatedRequests.class);
        var mediatedRequestIds = mediatedRequests.getMediatedRequests().stream().map(MediatedRequest::getId).toList();

        mockMvc.perform(
            get(URL_MEDIATED_BATCH_REQUESTS + "/" + BATCH_REQUEST_ID1 + "/details")
              .headers(headers)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("totalRecords", is(2)))
          .andExpect(jsonPath("mediatedBatchRequestDetails[*].confirmedRequestId", is(mediatedRequestIds)));
      });

    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(SEARCH_ITEM_URL + "/" + ITEM_IDS[0]))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(SEARCH_ITEM_URL + "/" + ITEM_IDS[1]))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(SETTINGS_ENTRIES_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_SECURE)));
  }

  // GET Batch Request Details tests

  @Test
  @SneakyThrows
  void shouldReturnsBatchRequestDetailsForGivenBatchId() {
    var dto = sampleBatchRequestPostDto(ITEM_IDS).batchId(BATCH_REQUEST_ID1);
    addStubForSettings();

    createBatchRequests(dto);
    var expectedPatronComment = dto.getPatronComments() + "\n\n\nBatch request ID: " + BATCH_REQUEST_ID1;

    var response = mockMvc.perform(
        get(URL_MEDIATED_BATCH_REQUESTS + "/" + BATCH_REQUEST_ID1 + "/details")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(2)))
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].mediatedRequestStatus",
        oneOf(PENDING.getValue(), IN_PROGRESS.getValue())))
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].requesterId", is(REQUESTER_ID)))
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].patronComments",
        is(expectedPatronComment)))
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].metadata.createdDate").exists())
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].metadata.createdByUsername").doesNotExist())
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].metadata.updatedDate").exists())
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].metadata.updatedByUsername").doesNotExist())
      .andReturn().getResponse().getContentAsString();
    var collectionDto = OBJECT_MAPPER.readValue(response, MediatedBatchRequestDetailsDto.class);

    assertThat(collectionDto.getMediatedBatchRequestDetails())
      .extracting(MediatedBatchRequestDetailDto::getBatchId, MediatedBatchRequestDetailDto::getItemId)
      .containsAll(List.of(
        tuple(BATCH_REQUEST_ID1, ITEM_IDS[0]),
        tuple(BATCH_REQUEST_ID1, ITEM_IDS[1])));
  }

  @Test
  @SneakyThrows
  void shouldReturnNotFoundForGetDetailsByBatchId() {
    mockMvc.perform(
        get(URL_MEDIATED_BATCH_REQUESTS + "/" + UUID.randomUUID() + "/details")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(exceptionMatch(MediatedBatchRequestNotFoundException.class));
  }

  @MethodSource("cqlDetailsQueryProvider")
  @ParameterizedTest(name = "query = ({0})")
  @DisplayName("Get Collection of Mediated Batch Requests Details by CQL query")
  @SneakyThrows
  void shouldReturnBatchRequestsDetailsForGivenQuery(String cql, int total, List<String> ids) {
    var itemId = UUID.randomUUID().toString();
    var postDto1 = sampleBatchRequestPostDto(ITEM_IDS).batchId(BATCH_REQUEST_ID1);
    var postDto2 = sampleBatchRequestPostDto(itemId).batchId(BATCH_REQUEST_ID2);

    addStubsForEcsRequestCreation(EXPECTED_CREATED_REQUEST_IDS, ITEM_IDS);
    addStubsForEcsRequestCreation(new UUID[]{EXPECTED_CREATED_REQUEST_IDS[0]}, new String[]{itemId});

    createBatchRequests(postDto1)
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID1)));
    createBatchRequests(postDto2)
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID2)));
    assertThat(batchRequestRepository.count()).isEqualTo(2L);
    assertThat(batchRequestSplitRepository.count()).isEqualTo(3L);

    Awaitility.await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE)
      .untilAsserted(() -> {
        assertThat(batchRequestRepository.findAll())
          .extracting(
            MediatedBatchRequest::getId,
            MediatedBatchRequest::getStatus)
          .containsExactlyInAnyOrder(
            tuple(UUID.fromString(BATCH_REQUEST_ID1), BatchRequestStatus.COMPLETED),
            tuple(UUID.fromString(BATCH_REQUEST_ID2), BatchRequestStatus.COMPLETED)
          );

        assertThat(batchRequestSplitRepository.findAll())
          .extracting(
            dto -> dto.getMediatedBatchRequest().getId().toString(),
            MediatedBatchRequestSplit::getItemId,
            MediatedBatchRequestSplit::getConfirmedRequestId,
            MediatedBatchRequestSplit::getPickupServicePointId)
          .containsExactlyInAnyOrder(
            tuple(BATCH_REQUEST_ID1, ITEM_IDS[0], EXPECTED_CREATED_REQUEST_IDS[0], SERVICE_POINT_IDS[0]),
            tuple(BATCH_REQUEST_ID1, ITEM_IDS[1], EXPECTED_CREATED_REQUEST_IDS[1], SERVICE_POINT_IDS[1]),
            tuple(BATCH_REQUEST_ID2, itemId, EXPECTED_CREATED_REQUEST_IDS[0], SERVICE_POINT_IDS[0])
          );
      });

    var response = getRequestsDetailsByQuery(cql)
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(total)))
      .andReturn().getResponse().getContentAsString();
    var collectionDto = OBJECT_MAPPER.readValue(response, MediatedBatchRequestDetailsDto.class);

    assertThat(collectionDto.getMediatedBatchRequestDetails())
      .extracting(MediatedBatchRequestDetailDto::getBatchId)
      .containsExactlyInAnyOrder(ids.toArray(new String[0]));
  }

  private static Stream<Arguments> cqlDetailsQueryProvider() {
    return Stream.of(
      arguments("", 3, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("requesterId==" + REQUESTER_ID, 3, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("itemId==" + ITEM_IDS[1], 1, List.of(BATCH_REQUEST_ID1)),
      arguments("pickupServicePointId==" + SERVICE_POINT_IDS[0], 2, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("confirmedRequestId==" + EXPECTED_CREATED_REQUEST_IDS[1], 1, List.of(BATCH_REQUEST_ID1)),
      arguments("confirmedRequestId==" + EXPECTED_CREATED_REQUEST_IDS[0] + " and itemId==" + ITEM_IDS[0], 1, List.of(BATCH_REQUEST_ID1)),
      arguments("mediatedRequestStatus==\"Completed\" ", 3, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("requestStatus==\"Open - Not yet filled\"", 3, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("createdDate<" + "2999-09-17T12:00:00.0", 3, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("updatedDate<" + "2024-09-17T12:00:00.0", 0, List.of())
    );
  }

  @SneakyThrows
  private ResultActions getAllRequests() {
    return mockMvc.perform(
      get(URL_MEDIATED_BATCH_REQUESTS)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  private ResultActions getRequestsByQuery(String query) {
    return mockMvc.perform(
      get(URL_MEDIATED_BATCH_REQUESTS)
        .queryParam("query", query)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  private ResultActions getRequestsDetailsByQuery(String query) {
    return mockMvc.perform(
      get(URL_MEDIATED_BATCH_REQUESTS + "/details")
        .queryParam("query", query)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  private ResultActions createBatchRequests(MediatedBatchRequestPostDto postDto) {
    return mockMvc.perform(post(URL_MEDIATED_BATCH_REQUESTS)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(postDto)))
      .andExpect(status().isCreated());
  }

  private static Stream<Arguments> cqlQueryProvider() {
    return Stream.of(
      arguments("mediatedRequestStatus==\"In progress\" or mediatedRequestStatus==\"Pending\"", 2, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("requesterId = " + REQUESTER_ID, 2, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("id = " + BATCH_REQUEST_ID1, 1, List.of(BATCH_REQUEST_ID1)),
      arguments("requestDate<" + "2999-09-17T12:00:00.0", 2, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("requestDate<" + "2024-09-17T12:00:00.0", 0, List.of())
    );
  }

  private MediatedBatchRequestPostDto sampleBatchRequestPostDto(String... itemIds) {
    var postDto = new MediatedBatchRequestPostDto()
      .requesterId(REQUESTER_ID)
      .mediatedWorkflow(MediatedBatchRequestPostDto.MediatedWorkflowEnum.MULTI_ITEM_REQUEST)
      .patronComments("batch patron comments");
    for (int i = 0; i < itemIds.length; i++) {
      postDto.getItemRequests().add(
        new MediatedBatchRequestPostDtoItemRequestsInner()
          .itemId(itemIds[i])
          .pickupServicePointId(SERVICE_POINT_IDS[i].toString()));
    }
    return postDto;
  }

  private void addStubsForSecureTenantRequestCreationInEcs(String[] itemIds) {
    mockGetUserTenants(TENANT_ID_SECURE, TENANT_ID_CONSORTIUM);

    for (int i = 0; i < itemIds.length; i++) {
      var item = new ConsortiumItem()
        .id(itemIds[i])
        .tenantId(TENANT_ID_COLLEGE)
        .instanceId(INSTANCE_ID)
        .holdingsRecordId(UUID.randomUUID().toString());

      wireMockServer.stubFor(WireMock.get(urlPathEqualTo(SEARCH_ITEM_URL + "/" + itemIds[i]))
        .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
        .willReturn(jsonResponse(item, HttpStatus.SC_OK)));

      addStubForSettings(TENANT_ID_SECURE);
    }
    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances" + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_SECURE))
      .willReturn(jsonResponse(new SearchInstancesResponse().addInstancesItem(
          new SearchInstance()
            .id(INSTANCE_ID)
            .tenantId(TENANT_ID_CONSORTIUM)),
        HttpStatus.SC_OK)));

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/instance-storage/instances/" + INSTANCE_ID))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new Instance().id(INSTANCE_ID), HttpStatus.SC_OK)));
  }

  private void mockGetUserTenants(String tenantId, String consortiumTenantId) {
    var userTenants = new UserTenantsResponse().userTenants(List.of(
      new UserTenant()
        .id(UUID.randomUUID())
        .userId(UUID.fromString("788fef93-43d1-4377-a7ba-24651cb0ee5c"))
        .username("consortia-system-user")
        .tenantId(tenantId)
        .centralTenantId(consortiumTenantId)
        .consortiumId(UUID.fromString("261a6ad9-0cab-47b5-ac83-a93b100d47b5"))
    )).totalRecords(1);
    wireMockServer.stubFor(WireMock.get(urlPathMatching( "/user-tenants"))
      .withHeader(HEADER_TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(userTenants, HttpStatus.SC_OK)));
  }

  private void addStubsForEcsRequestCreation(UUID[] expectedCreatedRequestIds, String[] itemIds) {
    for (int i = 0; i < expectedCreatedRequestIds.length; i++) {
      var item = new ConsortiumItem()
        .id(itemIds[i])
        .tenantId(TENANT_ID_CONSORTIUM)
        .instanceId(UUID.randomUUID().toString())
        .holdingsRecordId(UUID.randomUUID().toString());

      var ecsTrl = new EcsTlr().primaryRequestId(expectedCreatedRequestIds[i].toString());


      wireMockServer.stubFor(WireMock.get(urlPathEqualTo(SEARCH_ITEM_URL + "/" + itemIds[i]))
        .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
        .willReturn(jsonResponse(item, HttpStatus.SC_OK)));

      wireMockServer.stubFor(WireMock.post(ECS_TLR_EXTERNAL_URL)
        .withRequestBody(containing(itemIds[i]))
        .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
        .willReturn(jsonResponse(ecsTrl, HttpStatus.SC_CREATED)));

      addStubForSettings();
    }
  }

  private void addStubForSettings() {
    addStubForSettings(TENANT_ID_CONSORTIUM);
  }

  private void addStubForSettings(String tenantId) {
    var settingsEntries = new SettingsClient.SettingsEntries(List.of(
      new SettingsClient.SettingEntry(UUID.randomUUID(), SETTING_SCOPE, SETTING_KEY,
        new SettingsClient.BatchRequestItemsValidationValue(10))
    ),
      new SettingsClient.ResultInfo(1));

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(SETTINGS_ENTRIES_URL))
      .withQueryParam("query", containing(BATCH_REQUEST_ITEMS_VALIDATION_SETTING_FETCH_QUERY))
      .withQueryParam("limit", equalTo("1"))
      .withHeader(HEADER_TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(settingsEntries, HttpStatus.SC_OK)));
  }

  private MediatedBatchRequest createAndSaveBatchRequestEntity(BatchRequestStatus status) {
    var request = MediatedBatchRequest.builder()
      .id(UUID.fromString(BATCH_REQUEST_ID1))
      .requesterId(UUID.fromString(REQUESTER_ID))
      .status(status)
      .requestDate(Timestamp.from(Instant.now()))
      .build();

    return batchRequestRepository.save(request);
  }

  private void createAndSaveBatchRequestSplitEntity(MediatedBatchRequest batchRequest, BatchRequestSplitStatus status) {
    var split = MediatedBatchRequestSplit.builder()
      .id(UUID.randomUUID())
      .mediatedBatchRequest(batchRequest)
      .itemId(UUID.fromString(ITEM_IDS[0]))
      .requesterId(UUID.fromString(REQUESTER_ID))
      .pickupServicePointId(SERVICE_POINT_IDS[0])
      .status(status)
      .build();
    split.setCreatedDate(Timestamp.from(Instant.now()));
    split.setCreatedByUserId(UUID.randomUUID());
    split.setUpdatedDate(Timestamp.from(Instant.now()));
    split.setUpdatedByUserId(UUID.randomUUID());

    batchRequestSplitRepository.save(split);
  }
}
