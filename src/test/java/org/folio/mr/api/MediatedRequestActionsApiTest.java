package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Items;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

@IntegrationTest
class MediatedRequestActionsApiTest extends BaseIT {

  private static final String TENANT_ID_CENTRAL = "central";
  private static final String CONFIRM_ITEM_ARRIVAL_URL = "/requests-mediated/confirm-item-arrival";
  private static final String CONFIRM_MEDIATED_REQUEST_URL_TEMPLATE =
    "/requests-mediated/mediated-requests/%s/confirm";
  private static final String CIRCULATION_REQUESTS_URL = "/circulation/requests";
  private static final String ITEMS_URL = "/item-storage/items";
  private static final String ITEMS_BY_INSTANCE_URL = "/item-storage/items";
  private static final String INSTANCES_URL = "/instance-storage/instances";
  private static final String ECS_TLR_URL = "/tlr/ecs-tlr";

  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @BeforeEach
  public void beforeEach() {
    mediatedRequestsRepository.deleteAll();
  }

  @Test
  @SneakyThrows
  void mediatedRequestConfirmationForLocalInstanceAndItem() {
    MediatedRequestEntity initialRequest = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION));

    UUID circulationRequestId = UUID.randomUUID();
    UUID instanceId = initialRequest.getInstanceId();

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_REQUESTS_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new Request().id(circulationRequestId.toString()), HttpStatus.SC_OK)));

    confirmMediatedRequest(initialRequest.getId())
      .andExpect(status().isNoContent());

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(initialRequest.getId())
      .orElseThrow();
    assertThat(updatedRequest.getConfirmedRequestId(), is(circulationRequestId));

    wireMockServer.verify(getRequestedFor(urlMatching(INSTANCES_URL + "/" + instanceId))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(getRequestedFor(urlPathMatching(ITEMS_BY_INSTANCE_URL))
      .withQueryParam("query", equalTo("instanceId==\"" + instanceId + "\""))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(postRequestedFor(urlMatching(CIRCULATION_REQUESTS_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(ECS_TLR_URL)));
  }

  @Test
  @SneakyThrows
  void mediatedRequestConfirmationForLocalInstanceAndRemoteItem() {
    MediatedRequestEntity initialRequest = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION));

    wireMockServer.stubFor(WireMock.get(urlMatching(ITEMS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new Items().items(emptyList()), HttpStatus.SC_OK)));

    UUID primaryRequestId = UUID.randomUUID();
    EcsTlr ecsTlr = new EcsTlr().id(randomId())
      .primaryRequestId(primaryRequestId.toString());

    wireMockServer.stubFor(WireMock.post(urlMatching(ECS_TLR_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL))
      .willReturn(jsonResponse(ecsTlr, HttpStatus.SC_OK)));

    confirmMediatedRequest(initialRequest.getId())
      .andExpect(status().isNoContent());

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(initialRequest.getId())
      .orElseThrow();
    assertThat(updatedRequest.getConfirmedRequestId(), is(primaryRequestId));

    UUID instanceId = initialRequest.getInstanceId();
    wireMockServer.verify(getRequestedFor(urlMatching(INSTANCES_URL + "/" + instanceId))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(getRequestedFor(urlPathMatching(ITEMS_BY_INSTANCE_URL))
      .withQueryParam("query", equalTo("instanceId==\"" + instanceId + "\""))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(CIRCULATION_REQUESTS_URL)));
    wireMockServer.verify(postRequestedFor(urlMatching(ECS_TLR_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL)));
  }

  @Test
  @SneakyThrows
  void mediatedRequestConfirmationForRemoteInstanceAndItem() {
    UUID instanceId = UUID.randomUUID();
    UUID primaryRequestId = UUID.randomUUID();
    MediatedRequestEntity initialRequest = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION).withInstanceId(instanceId));

    EcsTlr ecsTlr = new EcsTlr().id(randomId())
      .primaryRequestId(primaryRequestId.toString());

    wireMockServer.stubFor(WireMock.post(urlMatching(ECS_TLR_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL))
      .willReturn(jsonResponse(ecsTlr, HttpStatus.SC_OK)));

    confirmMediatedRequest(initialRequest.getId())
      .andExpect(status().isNoContent());

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(initialRequest.getId())
      .orElseThrow();
    assertThat(updatedRequest.getConfirmedRequestId(), is(primaryRequestId));

    wireMockServer.verify(getRequestedFor(urlMatching(INSTANCES_URL + "/" + instanceId))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(ITEMS_BY_INSTANCE_URL)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(CIRCULATION_REQUESTS_URL)));
    wireMockServer.verify(postRequestedFor(urlMatching(ECS_TLR_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL)));
  }

  @Test
  @SneakyThrows
  void mediatedRequestConfirmationFailsForNonExistentRequest() {
    UUID mediatedRequestId = UUID.randomUUID();

    confirmMediatedRequest(mediatedRequestId)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request was not found: " + mediatedRequestId)));

    wireMockServer.verify(0, getRequestedFor(urlMatching(INSTANCES_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(ITEMS_BY_INSTANCE_URL)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(CIRCULATION_REQUESTS_URL)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(ECS_TLR_URL)));
  }

  @Test
  @SneakyThrows
  void successfulItemArrivalConfirmation() {
    MediatedRequestEntity request = createMediatedRequestEntity();

    confirmItemArrival("A14837334314")
      .andExpect(status().isOk())
      .andExpect(jsonPath("arrivalDate", notNullValue()))
      .andExpect(jsonPath("instance.id", is("69640328-788e-43fc-9c3c-af39e243f3b7")))
      .andExpect(jsonPath("instance.title", is("ABA Journal")))
      .andExpect(jsonPath("item.id", is("9428231b-dd31-4f70-8406-fe22fbdeabc2")))
      .andExpect(jsonPath("item.barcode", is("A14837334314")))
      .andExpect(jsonPath("item.enumeration", is("v.70:no.7-12")))
      .andExpect(jsonPath("item.volume", is("vol.1")))
      .andExpect(jsonPath("item.chronology", is("1984:July-Dec.")))
      .andExpect(jsonPath("item.displaySummary", is("test summary")))
      .andExpect(jsonPath("item.copyNumber", is("cp.1")))
      .andExpect(jsonPath("item.callNumberComponents.prefix", is("PFX")))
      .andExpect(jsonPath("item.callNumberComponents.callNumber", is("CN")))
      .andExpect(jsonPath("item.callNumberComponents.suffix", is("SFX")))
      .andExpect(jsonPath("mediatedRequest.id", is(request.getId().toString())))
      .andExpect(jsonPath("mediatedRequest.status", is("Open - Item arrived")))
      .andExpect(jsonPath("requester.id", is("9812e24b-0a66-457a-832c-c5e789797e35")))
      .andExpect(jsonPath("requester.barcode", is("111")))
      .andExpect(jsonPath("requester.firstName", is("Requester")))
      .andExpect(jsonPath("requester.middleName", is("X")))
      .andExpect(jsonPath("requester.lastName", is("Mediated")));

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(request.getId())
      .orElseThrow();
    assertThat(updatedRequest.getMediatedRequestStep(), is("Item arrived"));
    assertThat(updatedRequest.getStatus(), is("Open - Item arrived"));
  }

  @Test
  @SneakyThrows
  void itemArrivalConfirmationFailsWhenMediatedRequestIsNotFoundByItemBarcode() {
    confirmItemArrival("random-barcode")
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request for arrival confirmation of item with barcode 'random-barcode' was not found")));
  }

  @Test
  @SneakyThrows
  void itemArrivalConfirmationFailsWhenMediatedRequestIsNotFoundByStatus() {
    mediatedRequestsRepository.save(buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)); // wrong status

    confirmItemArrival("A14837334314")
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request for arrival confirmation of item with barcode 'A14837334314' was not found")));
  }

  private MediatedRequestEntity createMediatedRequestEntity() {
    return mediatedRequestsRepository.save(buildMediatedRequestEntity(OPEN_IN_TRANSIT_FOR_APPROVAL));
  }

  @SneakyThrows
  private ResultActions confirmItemArrival(String itemBarcode) {
    return mockMvc.perform(
      post(CONFIRM_ITEM_ARRIVAL_URL)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(new ConfirmItemArrivalRequest().itemBarcode(itemBarcode))));
  }

  @SneakyThrows
  private ResultActions confirmMediatedRequest(UUID mediatedRequestId) {
    return mockMvc.perform(
      post(format(CONFIRM_MEDIATED_REQUEST_URL_TEMPLATE, mediatedRequestId))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }
}
