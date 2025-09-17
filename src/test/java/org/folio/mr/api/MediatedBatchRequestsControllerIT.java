package org.folio.mr.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum.PENDING;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.groups.Tuple;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailsDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDto;
import org.folio.mr.domain.dto.MediatedBatchRequestPostDtoItemRequestsInner;
import org.folio.mr.domain.dto.MediatedBatchRequestsDto;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;


@IntegrationTest
public class MediatedBatchRequestsControllerIT extends BaseIT {

  private static final String URL_MEDIATED_BATCH_REQUESTS = "/requests-mediated/batch-mediated-requests";
  private static final String REQUESTER_ID = "9812e24b-0a66-457a-832c-c5e789797e35";
  private static final String BATCH_REQUEST_ID1 = UUID.randomUUID().toString();
  private static final String BATCH_REQUEST_ID2 = UUID.randomUUID().toString();
  private static final UUID[] ITEM_IDS = {UUID.randomUUID(), UUID.randomUUID()};

  @Autowired
  private MediatedBatchRequestRepository batchRequestRepository;

  @Autowired
  private MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @BeforeEach
  public void clearDatabase() {
    batchRequestSplitRepository.deleteAll();
    batchRequestRepository.deleteAll();
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
  void shouldReturnsNotFoundForGetById() {
    mockMvc.perform(
        get(URL_MEDIATED_BATCH_REQUESTS + "/" + UUID.randomUUID())
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @MethodSource("cqlQueryProvider")
  @ParameterizedTest(name = "query = ({0})")
  @DisplayName("Get Collection of Mediated Batch Requests by CQL query")
  @SneakyThrows
  void shouldReturnsBatchRequestsForGivenQuery(String cql, int total, List<String> ids) {
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
      .containsExactly(ids.toArray(new String[0]));
  }

  @Test
  @SneakyThrows
  void shouldCreateMediatedBatchRequest() {
    var postBatchRequestDto = sampleBatchRequestPostDto(2);

    createBatchRequests(postBatchRequestDto)
      .andExpect(jsonPath("mediatedRequestStatus", is(PENDING.getValue())))
      .andExpect(jsonPath("requesterId", is(REQUESTER_ID)))
      .andExpect(jsonPath("patronComments", is(postBatchRequestDto.getPatronComments())))
      .andExpect(jsonPath("mediatedWorkflow", is(postBatchRequestDto.getMediatedWorkflow())))
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
    var postBatchRequestDto = sampleBatchRequestPostDto(1)
      .batchId(BATCH_REQUEST_ID1);

    createBatchRequests(postBatchRequestDto)
      .andExpect(jsonPath("mediatedRequestStatus", is(PENDING.getValue())))
      .andExpect(jsonPath("batchId", is(BATCH_REQUEST_ID1)));

    assertThat(batchRequestRepository.count()).isEqualTo(1L);
    assertThat(batchRequestSplitRepository.count()).isEqualTo(1L);
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
      .andExpect(jsonPath("mediatedBatchRequestDetails[0].mediatedRequestStatus", is(PENDING.getValue())))
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
      .andExpect(status().isNotFound());
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
      .mediatedWorkflow("Batch workflow")
      .patronComments("batch patron comments");
    for (int i = 0; i < itemsCount; i++) {
      var randomUUID = UUID.randomUUID().toString();
      postDto.getItemRequests().add(
        new MediatedBatchRequestPostDtoItemRequestsInner().itemId(ITEM_IDS[i].toString()).pickupServicePointId(randomUUID));
    }
    return postDto;
  }
}
