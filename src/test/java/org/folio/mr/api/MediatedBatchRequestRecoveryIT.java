package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.stream.IntStream.range;
import static org.folio.mr.api.BaseIT.TENANT_ID_SECURE;
import static org.folio.mr.domain.BatchRequestSplitStatus.COMPLETED;
import static org.folio.mr.domain.BatchRequestSplitStatus.FAILED;
import static org.folio.mr.domain.BatchRequestSplitStatus.PENDING;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.dto.IdentifiableMediatedBatchSplit;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto.MediatedWorkflowEnum;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.test.types.IntegrationTest;

@IntegrationTest
@DatabaseCleanup(tables = { "batch_request_split", "batch_request", "mediated_request" })
@SpringBootTest(properties = {
  "folio.tenant.secure-tenant-id=" + TENANT_ID_SECURE,
  "folio.batch-requests.stale-request-threshold=5m",
  "folio.batch-requests.stale-requests-query-limit=2",
})
class MediatedBatchRequestRecoveryIT extends MediatedBatchRequestBaseIT {

  public static final String BATCH_REQUEST_BY_ID_URL = "/requests-mediated/batch-mediated-requests/{id}";
  @Autowired private FolioModuleMetadata folioModuleMetadata;
  @Autowired private MediatedBatchRequestsService batchRequestsService;
  @Autowired private MediatedBatchRequestSplitService batchRequestSplitService;
  @Autowired private MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @BeforeEach
  void setUp() {
    setUpTenant(TENANT_ID_SECURE);
  }

  @Test
  @SneakyThrows
  void recoverStaleRequests_positive_staleBatchWithAllPendingItemRequests() {
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_CONSORTIUM);
    var batch = createNewBatchRequest(2);
    var firstSplit = batch.splits().getFirst();
    var secondSplit = batch.splits().getLast();
    updateBatch(batch.batchId(), BatchRequestStatus.PENDING, true);
    updateBatchSplit(firstSplit.id(), PENDING, true);
    updateBatchSplit(secondSplit.id(), PENDING, true);

    recoverStaleBatchRequests();

    awaitUntilAsserted(() -> verifyRequestCompletion(batch.batchId()));
    wireMockServer.verify(1, postRequestedFor(urlPathMatching("/circulation/requests"))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(firstSplit.mediatedBatchRequest().getItemId())))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));

    wireMockServer.verify(1, postRequestedFor(urlPathMatching("/circulation/requests"))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(secondSplit.mediatedBatchRequest().getItemId())))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void recoverStaleRequests_positive_nonStaleBatchWithAllPendingItemRequests() {
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_CONSORTIUM);
    var batch = createNewBatchRequest(2);

    recoverStaleBatchRequests();

    Awaitility.await()
      .pollDelay(Durations.FIVE_HUNDRED_MILLISECONDS)
      .atMost(Durations.FIVE_SECONDS)
      .untilAsserted(() -> mockMvc.perform(get(BATCH_REQUEST_BY_ID_URL, batch.batchId())
          .headers(defaultHeaders(TENANT_ID_CONSORTIUM))
          .contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("mediatedRequestStatus", is("Pending"))));

    wireMockServer.verify(0, postRequestedFor(urlPathMatching("/circulation/requests"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void recoverStaleRequests_positive_staleBatchWithTwoCompletedItemRequests() {
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_CONSORTIUM);
    var batch = createNewBatchRequest(2);
    var firstSplit = batch.splits().getFirst();
    var secondSplit = batch.splits().getLast();
    updateBatch(batch.batchId(), BatchRequestStatus.IN_PROGRESS, true);
    updateBatchSplit(firstSplit.id(), COMPLETED, true);
    updateBatchSplit(secondSplit.id(), FAILED, true);

    recoverStaleBatchRequests();

    awaitUntilAsserted(() -> verifyRequestCompletion(batch.batchId()));
    wireMockServer.verify(0, postRequestedFor(urlPathMatching("/circulation/requests"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void recoverStaleRequests_positive_staleBatchWithOneCompletedAndOnePendingItemRequests() {
    setupWiremockForSingleTenantRequestCreation(TENANT_ID_CONSORTIUM);
    var batch = createNewBatchRequest(2);
    var firstSplit = batch.splits().getFirst();
    var lastSplit = batch.splits().getLast();
    updateBatch(batch.batchId(), BatchRequestStatus.IN_PROGRESS, true);
    updateBatchSplit(firstSplit.id(), COMPLETED, true);
    updateBatchSplit(lastSplit.id(), PENDING, true);

    recoverStaleBatchRequests();

    awaitUntilAsserted(() -> verifyRequestCompletion(batch.batchId()));
    wireMockServer.verify(0, postRequestedFor(urlPathMatching("/circulation/requests"))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(firstSplit.mediatedBatchRequest().getItemId())))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));

    wireMockServer.verify(1, postRequestedFor(urlPathMatching("/circulation/requests"))
      .withRequestBody(matchingJsonPath("$.itemId", equalTo(lastSplit.mediatedBatchRequest().getItemId())))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @SneakyThrows
  private void verifyRequestCompletion(String batchId) {
    mockMvc.perform(get("/requests-mediated/batch-mediated-requests/{id}", batchId)
        .headers(defaultHeaders(BaseIT.TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("mediatedRequestStatus", anyOf(is("Completed"), is("Failed"))));
  }

  @SneakyThrows
  private void recoverStaleBatchRequests() {
    mockMvc.perform(post("/batch-mediated-requests-recovery")
        .headers(defaultHeaders(TENANT_ID_CONSORTIUM)))
      .andExpect(status().isOk());
  }

  private CreatedBatchRequestHolder createNewBatchRequest(int splitNum) {
    var okapiHeaders = Map.<String, Collection<String>>of(
      HEADER_TENANT, List.of(BaseIT.TENANT_ID_CONSORTIUM),
      XOkapiHeaders.USER_ID, List.of(REQUESTER_ID));
    try (var ignored = new FolioExecutionContextSetter(folioModuleMetadata, okapiHeaders)) {
      var itemRequests = range(0, splitNum)
        .mapToObj(idx -> new MediatedBatchRequestPostDtoItemRequestsInner()
          .pickupServicePointId(SERVICE_POINT_ID)
          .itemId(UUID.randomUUID().toString()))
        .toList();

      var requestBody = new MediatedBatchRequestPostDto()
        .batchId(UUID.randomUUID().toString())
        .requesterId(REQUESTER_ID)
        .patronComments("Batch request recovery test comment")
        .mediatedWorkflow(MediatedWorkflowEnum.MULTI_ITEM_REQUEST)
        .itemRequests(itemRequests);

      var createdBatch = batchRequestsService.create(requestBody);
      var batchId = UUID.fromString(createdBatch.getBatchId());
      var allByBatchId = batchRequestSplitService.getAllByBatchId(batchId);
      return new CreatedBatchRequestHolder(createdBatch.getBatchId(), createdBatch, allByBatchId);
    }
  }

  @SuppressWarnings("SqlSourceToSinkFlow")
  private void updateBatch(String id, BatchRequestStatus status, boolean markAsStale) {
    var newUpdatedDateClause = markAsStale ? "created_date - INTERVAL '25 Minutes'" : "created_date";
    var tableName = databaseHelper.getDbPath(TENANT_ID_CONSORTIUM, "batch_request");
    var sql = """
      UPDATE %1$s batch
      SET status = CAST(? AS BatchRequestStatus),
          last_processed_at = %2$s,
          created_date = %2$s,
          updated_date = %2$s,
          updated_by_user_id = ?
      WHERE batch.id = ?
      """.formatted(tableName, newUpdatedDateClause);
    jdbcTemplate.update(sql, status.getValue(), UUID.fromString(REQUESTER_ID), UUID.fromString(id));
  }

  @SuppressWarnings("SqlSourceToSinkFlow")
  private void updateBatchSplit(UUID splitId, BatchRequestSplitStatus status, boolean markAsStale) {
    var newUpdatedDateClause = markAsStale ? "created_date - INTERVAL '25 Minutes'" : "created_date";
    var tableName = databaseHelper.getDbPath(TENANT_ID_CONSORTIUM, "batch_request_split");
    var sql = """
      UPDATE %1$s split
      SET status = CAST(? AS BatchRequestSplitStatus),
          created_date = %2$s,
          updated_date = %2$s,
          updated_by_user_id = ?
      WHERE split.id = ?
      """.formatted(tableName, newUpdatedDateClause);
    jdbcTemplate.update(sql, status.getValue(), UUID.fromString(REQUESTER_ID), splitId);

  }

  record CreatedBatchRequestHolder(
    String batchId,
    MediatedBatchRequestDto batch,
    List<IdentifiableMediatedBatchSplit> splits) {
  }
}
