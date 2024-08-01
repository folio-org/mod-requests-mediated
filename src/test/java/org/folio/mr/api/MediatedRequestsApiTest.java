package org.folio.mr.api;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.test.types.IntegrationTest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import lombok.SneakyThrows;

@IntegrationTest
class MediatedRequestsApiTest extends BaseIT {
  private static final String URI_TEMPLATE = "/requests-mediated/mediated-requests";
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
    doPost(URI_TEMPLATE, buildMediatedRequestPayload())
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
    doPost(URI_TEMPLATE, buildMediatedRequestPayload()).expectStatus().isEqualTo(CREATED);
    mockMvc.perform(
        get(URI_TEMPLATE + "?query=requestType==Hold")
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeCreatedAndRetrievedById() {
    var response = doPost(URI_TEMPLATE, buildMediatedRequestPayload())
      .expectStatus().isEqualTo(CREATED);
    var requestId = getResponseBodyObjectId(response);
    mockMvc.perform(get(URI_TEMPLATE + "/" + requestId)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("instance.identifiers", hasSize(2)))
      .andExpect(jsonPath("instance.identifiers[*].value",
        containsInAnyOrder("0123456789", "98765432123456")))
      .andExpect(jsonPath("item.barcode", is("12345")))
      .andExpect(jsonPath("requester.barcode", is("123")))
      .andExpect(jsonPath("searchIndex.callNumberComponents.callNumber", is("F16.H37 A2 9001")))
      .andExpect(jsonPath("searchIndex.callNumberComponents.prefix", is("pre")))
      .andExpect(jsonPath("searchIndex.callNumberComponents.suffix", is("suf")))
      .andExpect(jsonPath("searchIndex.shelvingOrder", is("F 416 H37 A2 59001")))
      .andExpect(jsonPath("searchIndex.pickupServicePointName", is("Circ Desk 1")))
      .andExpect(jsonPath("metadata.createdDate", notNullValue()))
      .andExpect(jsonPath("metadata.updatedDate", notNullValue()));
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeCreatedAndRetrievedByQuery() {
    var response = doPost(URI_TEMPLATE, buildMediatedRequestPayload())
      .expectStatus().isEqualTo(CREATED);
    var requestId = getResponseBodyObjectId(response);

    mockMvc.perform(get(URI_TEMPLATE + "?query=fullCallNumber==\"*reF16*\"")
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("mediatedRequests[0].id", is(requestId)));
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeUpdated() {
    var response = doPost(URI_TEMPLATE, buildMediatedRequestPayload())
      .expectStatus().isEqualTo(CREATED);
    var requestId = getResponseBodyObjectId(response);
    mockMvc.perform(
        put(URI_TEMPLATE + "/" + requestId)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .content(buildMediatedRequestPayload()))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldNotBeUpdatedWhenNotFound() {
    mockMvc.perform(
        put(URI_TEMPLATE + "/" + randomId())
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .content(buildMediatedRequestPayload()))
      .andExpect(status().isNotFound());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeCreatedAndDeleted() {
    var response = doPost(URI_TEMPLATE, buildMediatedRequestPayload())
      .expectStatus().isEqualTo(CREATED);
    var requestId = getResponseBodyObjectId(response);
    mockMvc.perform(
        delete(URI_TEMPLATE + "/" + requestId)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldNoteBeFoundForDelete() {
    mockMvc.perform(
        delete(URI_TEMPLATE + "/" + randomId())
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  private static String buildMediatedRequestPayload() {
    return """
      {
        "id": "69abbd7d-b4a6-4376-8453-bf349c579a91",
        "requestLevel": "Title",
        "requestType": "Hold",
        "requestDate": "2024-01-01T00:12:00Z",
        "patronComments": "",
        "requesterId": "73b17dee-d278-4128-b0fd-5d9f801b0400",
        "requester": {
          "firstName": "First",
          "lastName": "Last",
          "middleName": "Middle",
          "barcode": "123"
        },
        "proxyUserId": "13e309c8-d501-4c3b-9cbc-d573621bb62c",
        "proxy": {
          "firstName": "ProxyFirst",
          "lastName": "ProxyLast",
          "middleName": "ProxyMiddle",
          "barcode": "Proxy123"
        },
        "instanceId": "6d996a75-930d-4ad1-a953-9b44c9021a35",
        "instance": {
          "title": "Children of Time",
          "identifiers": [
            {
              "value": "0123456789",
              "identifierTypeId": "8261054f-9876-422d-bd51-4ed9f33c7654"
            },
            {
              "value": "98765432123456",
              "identifierTypeId": "8261054f-9876-422d-bd51-4ed9f33c7654"
            }
          ]
        },
        "holdingsRecordId": null,
        "itemId": null,
        "item": {
          "barcode": "12345"
        },
        "mediatedWorkflow": "Private request",
        "mediatedRequestStatus": "New",
        "status": "New - Awaiting confirmation",
        "cancellationReasonId": null,
        "cancelledByUserId": null,
        "cancellationAdditionalInformation": null,
        "cancelledDate": null,
        "position": 1,
        "fulfillmentPreference": "Hold Shelf",
        "deliveryAddressTypeId": null,
        "pickupServicePointId": "b2ffa7df-98e8-48a1-b5a8-4e712364eb8d",
        "searchIndex": {
          "callNumberComponents": {
            "callNumber": "F16.H37 A2 9001",
            "prefix": "pre",
            "suffix": "suf"
          },
          "shelvingOrder": "F 416 H37 A2 59001",
          "pickupServicePointName": "Circ Desk 1"
        }
      }
      """;
  }

  @SneakyThrows
  private static String getResponseBodyObjectId(WebTestClient.ResponseSpec responseSpec) {
    var byteArray = responseSpec.returnResult(MediatedRequest.class).getResponseBodyContent();
    return new JSONObject(new String(byteArray == null ? new byte[0] : byteArray)).getString("id");
  }
}
