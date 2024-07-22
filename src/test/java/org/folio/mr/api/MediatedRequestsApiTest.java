package org.folio.mr.api;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.folio.mr.repository.MediatedRequestsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import lombok.SneakyThrows;

class MediatedRequestsApiTest extends BaseIT {
  private static final String URI_TEMPLATE = "/requests-mediated/mediated-requests";
  private static final String REQUEST_ID = "8079f233-e08c-4225-b438-0747e4838262";
  private static final String TEST_PAYLOAD = "{\n" +
    "  \"id\": \"" + REQUEST_ID + "\",\n" +
    "  \"requestLevel\": \"Title\",\n" +
    "  \"requestType\": \"Hold\",\n" +
    "  \"requestDate\": \"2024-01-01T00:12:00+00:00\",\n" +
    "  \"patronComments\": \"\",\n" +
    "  \"requesterId\": \"73b17dee-d278-4128-b0fd-5d9f801b0400\",\n" +
    "  \"proxyUserId\": \"13e309c8-d501-4c3b-9cbc-d573621bb62c\",\n" +
    "  \"instanceId\": \"6d996a75-930d-4ad1-a953-9b44c9021a35\",\n" +
    "  \"holdingsRecordId\": null,\n" +
    "  \"itemId\": null,\n" +
    "  \"status\": \"New - Awaiting confirmation\",\n" +
    "  \"cancellationReasonId\": null,\n" +
    "  \"cancelledByUserId\": null,\n" +
    "  \"cancellationAdditionalInformation\": null,\n" +
    "  \"cancelledDate\": null,\n" +
    "  \"position\": 1,\n" +
    "  \"fulfillmentPreference\": \"Hold Shelf\",\n" +
    "  \"deliveryAddressTypeId\": null,\n" +
    "  \"pickupServicePointId\": \"b2ffa7df-98e8-48a1-b5a8-4e712364eb8d\"\n" +
    "}";
  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @BeforeEach
  public void clearDatabase() {
    mediatedRequestsRepository.deleteAll();
  }
  @Test
  void getByIdNotFound() throws Exception {
    mockMvc.perform(
        get(URI_TEMPLATE + UUID.randomUUID())
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  void mediatedRequestShouldBeCreated() {
    doPost(URI_TEMPLATE, TEST_PAYLOAD)
      .expectStatus().isEqualTo(CREATED);
  }

  @SneakyThrows
  @Test
  void mediatedRequestsShouldBeRetrieved() {
    mockMvc.perform(
        get(URI_TEMPLATE)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @SneakyThrows
  @Test
  void mediatedRequestsShouldBeRetrievedByQuery() {
    doPost(URI_TEMPLATE, TEST_PAYLOAD)
      .expectStatus().isEqualTo(CREATED);
    mockMvc.perform(
        get(URI_TEMPLATE + "?query=requestType==Hold")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeCreatedAndRetrievedById() {
    doPost(URI_TEMPLATE, TEST_PAYLOAD).expectStatus().isEqualTo(CREATED);
    mockMvc.perform(
        get(URI_TEMPLATE + "/" + REQUEST_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeUpdated() {
    doPost(URI_TEMPLATE, TEST_PAYLOAD).expectStatus().isEqualTo(CREATED);
    mockMvc.perform(
        put(URI_TEMPLATE + "/" + REQUEST_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .content(TEST_PAYLOAD))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeUpdatedAndNotFound() {
    mockMvc.perform(
        put(URI_TEMPLATE + "/" + REQUEST_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .content(TEST_PAYLOAD))
      .andExpect(status().isNotFound());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeCreatedAndDeleted() {
    doPost(URI_TEMPLATE, TEST_PAYLOAD).expectStatus().isEqualTo(CREATED);
    mockMvc.perform(
        delete(URI_TEMPLATE + "/" + REQUEST_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldNoteBeFoundForDelete() {
    mockMvc.perform(
        delete(URI_TEMPLATE + "/" + REQUEST_ID)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }
}
