package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.ONE_SECOND;
import static org.folio.mr.api.BaseIT.TENANT_ID_SECURE;
import static org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum.IN_PROGRESS;
import static org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum.PENDING;
import static org.folio.mr.domain.type.ErrorCode.DUPLICATE_BATCH_REQUEST_ID;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.assertj.core.groups.Tuple;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.UserTenant;
import org.folio.mr.domain.dto.UserTenantsResponse;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.mr.service.flow.BatchFailedFlowFinalizer;
import org.folio.mr.service.flow.BatchFlowFinalizer;
import org.folio.mr.service.flow.BatchFlowInitializer;
import org.folio.mr.service.flow.BatchSplitProcessor;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
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
  private static final String REQUESTER_ID = "9812e24b-0a66-457a-832c-c5e789797e35";
  private static final String BATCH_REQUEST_ID1 = UUID.randomUUID().toString();
  private static final String BATCH_REQUEST_ID2 = UUID.randomUUID().toString();
  private static final UUID[] ITEM_IDS = {UUID.randomUUID(), UUID.randomUUID()};
  private static final String SEARCH_ITEM_URL = "/search/consortium/item";
  private static final String ECS_TLR_EXTERNAL_URL = "/tlr/ecs-tlr/create-ecs-request-external";
  private static final String CIRCULATION_REQUEST_URL = "/circulation/requests";

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

    var userTenants = new UserTenantsResponse().userTenants(List.of(
      new UserTenant()
        .id(UUID.randomUUID())
        .userId(UUID.fromString("788fef93-43d1-4377-a7ba-24651cb0ee5c"))
        .username("consortia-system-user")
        .tenantId("consortium")
        .centralTenantId("consortium")
        .consortiumId(UUID.fromString("261a6ad9-0cab-47b5-ac83-a93b100d47b5"))
    )).totalRecords(1);
    wireMockServer.stubFor(WireMock.get(urlPathMatching( "/user-tenants"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(userTenants, HttpStatus.SC_OK)));
  }

  @Test
  @SneakyThrows
  void getReturnsEmptyArrayWhenNoRequestsAreFound() {
    getAllRequests()
      .andExpect(status().isOk())
      .andExpect(jsonPath("mediatedBatchRequests", emptyIterable()))
      .andExpect(jsonPath("totalRecords", is(0)));
  }

  @Test
  @SneakyThrows
  void shouldReturnBatchRequestById() {
    var dto = sampleBatchRequestPostDto(1).batchId(BATCH_REQUEST_ID1);
    createBatchRequests(dto);

    mockMvc.perform(
      get(URL_MEDIATED_BATCH_REQUESTS + "/" + BATCH_REQUEST_ID1)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID1)));
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

  @MethodSource("cqlQueryProvider")
  @ParameterizedTest(name = "query = ({0})")
  @DisplayName("Get Collection of Mediated Batch Requests by CQL query")
  @SneakyThrows
  void shouldReturnBatchRequestsForGivenQuery(String cql, int total, List<String> ids) {
    var postDto1 = sampleBatchRequestPostDto(1).batchId(BATCH_REQUEST_ID1);
    var postDto2 = sampleBatchRequestPostDto(1).batchId(BATCH_REQUEST_ID2);
    createBatchRequests(postDto1);
    createBatchRequests(postDto2);

    var response = getRequestsByQuery(cql)
      .andExpect(status().isOk())
      .andExpect(jsonPath("totalRecords", is(total)))
      .andReturn().getResponse().getContentAsString();
    var collectionDto = OBJECT_MAPPER.readValue(response, MediatedBatchRequestsDto.class);

    assertThat(collectionDto.getMediatedBatchRequests())
      .extracting(MediatedBatchRequestDto::getBatchId)
      .containsExactlyInAnyOrder(ids.toArray(new String[0]));
  }

  @Test
  @SneakyThrows
  void shouldCreateMediatedBatchRequest() {
    var postBatchRequestDto = sampleBatchRequestPostDto(2);

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
    var postBatchRequestDto = sampleBatchRequestPostDto(2)
      .batchId(BATCH_REQUEST_ID1);

    var expectedRequestId = UUID.randomUUID();
    addStubsForEcsRequestCreation(expectedRequestId);

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
          .extracting(MediatedBatchRequestSplit::getStatus, MediatedBatchRequestSplit::getConfirmedRequestId)
          .containsOnly(Tuple.tuple(BatchRequestSplitStatus.COMPLETED, expectedRequestId));
        assertEquals(BatchRequestStatus.COMPLETED, batchRequestRepository.findAll().getFirst().getStatus());
      });
  }

  @Test
  @SneakyThrows
  void shouldHandleBatchSplitProcessingFailureAndMarkBatchSplitFailed() {
    var postBatchRequestDto = sampleBatchRequestPostDto(2)
      .batchId(BATCH_REQUEST_ID1);
    doThrow(new RuntimeException("request placing failed"))
      .when(batchSplitProcessor).execute(any(BatchSplitContext.class));

    createBatchRequests(postBatchRequestDto)
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID1)));

    var captor = ArgumentCaptor.forClass(BatchContext.class);
    Awaitility.await().pollInterval(ONE_SECOND).atMost(Durations.ONE_MINUTE)
      .untilAsserted(() -> {
        verify(flowInitializer).execute(captor.capture());
        var batchContext = captor.getValue();
        assertEquals(BATCH_REQUEST_ID1, batchContext.getBatchRequestId().toString());
        assertThat(batchContext.getBatchSplitEntitiesById()).hasSize(2);

        verify(failedFlowFinalizer).execute(any(BatchContext.class));
      });

    assertThat(batchRequestSplitRepository.findAll())
      .extracting(MediatedBatchRequestSplit::getStatus)
      .containsOnly(BatchRequestSplitStatus.FAILED);

    var batchRequest = batchRequestRepository.findById(UUID.fromString(BATCH_REQUEST_ID1)).orElseThrow();
    assertThat(batchRequest.getStatus()).isEqualTo(BatchRequestStatus.FAILED);
  }

  @Test
  @SneakyThrows
  void shouldThrowErrorOnCreateMediatedBatchRequest_BatchAlreadyExistsWithSameId() {
    var postBatchRequestDto1 = sampleBatchRequestPostDto(1)
      .batchId(BATCH_REQUEST_ID1);
    var postBatchRequestDto2 = sampleBatchRequestPostDto(2)
      .batchId(BATCH_REQUEST_ID1);
    createBatchRequests(postBatchRequestDto1);

    mockMvc.perform(post(URL_MEDIATED_BATCH_REQUESTS)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(postBatchRequestDto2)))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(errorMessageMatch(is(DUPLICATE_BATCH_REQUEST_ID.getMessage())))
      .andExpect(exceptionMatch(DataIntegrityViolationException.class));
  }

  @Test
  @SneakyThrows
  void shouldReturnsBatchRequestDetailsForGivenBatchId() {
    var dto = sampleBatchRequestPostDto(2).batchId(BATCH_REQUEST_ID1);
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
        Tuple.tuple(BATCH_REQUEST_ID1, ITEM_IDS[0].toString()),
        Tuple.tuple(BATCH_REQUEST_ID1, ITEM_IDS[1].toString())));
  }

  @Test
  @SneakyThrows
  void shouldReturnsNotFoundForGetDetailsByBatchId() {
    mockMvc.perform(
        get(URL_MEDIATED_BATCH_REQUESTS + "/" + UUID.randomUUID() + "/details")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound())
      .andExpect(exceptionMatch(MediatedBatchRequestNotFoundException.class));
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
  private ResultActions createBatchRequests(MediatedBatchRequestPostDto postDto) {
    return mockMvc.perform(post(URL_MEDIATED_BATCH_REQUESTS)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(postDto)))
      .andExpect(status().isCreated());
  }

  private static Stream<Arguments> cqlQueryProvider() {
    return Stream.of(
      arguments("requesterId = " + REQUESTER_ID, 2, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("id = " + BATCH_REQUEST_ID1, 1, List.of(BATCH_REQUEST_ID1)),
      arguments("requestDate<" + "2999-09-17T12:00:00.0", 2, List.of(BATCH_REQUEST_ID1, BATCH_REQUEST_ID2)),
      arguments("requestDate<" + "2024-09-17T12:00:00.0", 0, List.of())
    );
  }

  private MediatedBatchRequestPostDto sampleBatchRequestPostDto(int itemsCount) {
    var postDto = new MediatedBatchRequestPostDto()
      .requesterId(REQUESTER_ID)
      .mediatedWorkflow(MediatedBatchRequestPostDto.MediatedWorkflowEnum.MULTI_ITEM_REQUEST)
      .patronComments("batch patron comments");
    for (int i = 0; i < itemsCount; i++) {
      var randomUUID = UUID.randomUUID().toString();
      postDto.getItemRequests().add(
        new MediatedBatchRequestPostDtoItemRequestsInner().itemId(ITEM_IDS[i].toString()).pickupServicePointId(randomUUID));
    }
    return postDto;
  }

  private void addStubsForEcsRequestCreation(UUID expectedRequestId) {
    var item1 = new ConsortiumItem()
      .id(ITEM_IDS[0].toString())
      .tenantId(TENANT_ID_CONSORTIUM)
      .instanceId(UUID.randomUUID().toString())
      .holdingsRecordId(UUID.randomUUID().toString());
    var item2 = new ConsortiumItem()
      .id(ITEM_IDS[1].toString())
      .tenantId(TENANT_ID_CONSORTIUM)
      .instanceId(UUID.randomUUID().toString())
      .holdingsRecordId(UUID.randomUUID().toString());
//    var userTenants = new UserTenantsResponse().userTenants(List.of(
//      new UserTenant()
//        .id(UUID.randomUUID())
//        .userId(UUID.fromString("788fef93-43d1-4377-a7ba-24651cb0ee5c"))
//        .username("consortia-system-user")
//        .tenantId("consortium")
//        .centralTenantId("consortium")
//        .consortiumId(UUID.fromString("261a6ad9-0cab-47b5-ac83-a93b100d47b5"))
//    )).totalRecords(1);

    var ecsTrl = new EcsTlr().primaryRequestId(expectedRequestId.toString());

//    wireMockServer.stubFor(WireMock.get(urlPathMatching( "/user-tenants"))
//      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
//      .willReturn(jsonResponse(userTenants, HttpStatus.SC_OK)));

    wireMockServer.stubFor(WireMock.get(urlEqualTo(SEARCH_ITEM_URL + "/" + ITEM_IDS[0]))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(item1, HttpStatus.SC_OK)));

    wireMockServer.stubFor(WireMock.get(urlEqualTo(SEARCH_ITEM_URL + "/" + ITEM_IDS[1]))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(item2, HttpStatus.SC_OK)));

    wireMockServer.stubFor(WireMock.post(ECS_TLR_EXTERNAL_URL)
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(ecsTrl, HttpStatus.SC_CREATED)));

    wireMockServer.stubFor(WireMock.get(urlMatching(CIRCULATION_REQUEST_URL + "/" + expectedRequestId))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new Request().id(expectedRequestId.toString()), HttpStatus.SC_OK)));
  }
}
