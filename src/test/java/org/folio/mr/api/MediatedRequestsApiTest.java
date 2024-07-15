package org.folio.mr.api;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import lombok.SneakyThrows;

class MediatedRequestsApiTest extends BaseIT {
  private static final String URI_TEMPLATE = "/requests-mediated/mediated-requests";

  @Test
  void getByIdNotFound() throws Exception {
    mockMvc.perform(
        get(URI_TEMPLATE + UUID.randomUUID())
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void testPost() {
    doPost(URI_TEMPLATE, "{\n" +
      "  \"id\": \"69abbd7d-b4a6-4376-8453-bf349c579a91\",\n" +
      "  \"requestLevel\": \"Title\",\n" +
      "  \"requestType\": \"Hold\",\n" +
      "  \"requestDate\": \"2024-01-01T00:12:00+00:00\",\n" +
      "  \"patronComments\": \"\",\n" +
      "  \"requesterId\": \"73b17dee-d278-4128-b0fd-5d9f801b0400\",\n" +
      "  \"proxyUserId\": \"13e309c8-d501-4c3b-9cbc-d573621bb62c\",\n" +
      "  \"instanceId\": \"6d996a75-930d-4ad1-a953-9b44c9021a35\",\n" +
      "  \"itemId\": null,\n" +
      "  \"status\": \"Open - Not yet filled\",\n" +
      "  \"cancellationReasonId\": null,\n" +
      "  \"cancelledByUserId\": null,\n" +
      "  \"cancellationAdditionalInformation\": null,\n" +
      "  \"cancelledDate\": null,\n" +
      "  \"position\": 1,\n" +
      "  \"fulfillmentPreference\": \"Hold Shelf\",\n" +
      "  \"deliveryAddressTypeId\": null,\n" +
      "  \"pickupServicePointId\": \"b2ffa7df-98e8-48a1-b5a8-4e712364eb8d\"\n" +
      "}")
      .expectStatus().isEqualTo(CREATED);
  }
}
