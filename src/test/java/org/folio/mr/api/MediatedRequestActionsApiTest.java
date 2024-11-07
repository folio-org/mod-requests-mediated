package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.NEW_AWAITING_CONFIRMATION;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Items;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.dto.SearchItem;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.entity.MediatedRequestStep;
import org.folio.mr.repository.MediatedRequestWorkflowLogRepository;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

@IntegrationTest
class MediatedRequestActionsApiTest extends BaseIT {

  private static final String TENANT_ID_CENTRAL = "central";
  private static final String CONFIRM_ITEM_ARRIVAL_URL = "/requests-mediated/confirm-item-arrival";
  private static final String SEND_ITEM_IN_TRANSIT_URL = "/requests-mediated/send-item-in-transit";
  private static final String CONFIRM_MEDIATED_REQUEST_URL_TEMPLATE =
    "/requests-mediated/mediated-requests/%s/confirm";
  private static final String DECLINE_MEDIATED_REQUEST_URL_TEMPLATE =
    "/requests-mediated/mediated-requests/%s/decline";
  private static final String CIRCULATION_REQUESTS_URL = "/circulation/requests";
  private static final String ITEMS_URL = "/item-storage/items";
  private static final String INSTANCES_URL = "/instance-storage/instances";
  private static final String ECS_TLR_URL = "/tlr/ecs-tlr";
  private static final String SEARCH_ITEMS_URL = "/search/consortium/items";
  private static final String NOT_FOUND_ITEM_UUID = "f13ef24f-d0fe-4aa8-901a-bfad3f0e6cae";
  private static final String SEARCH_INSTANCES_URL = "/search/instances";

  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @Autowired
  private MediatedRequestWorkflowLogRepository workflowLogRepository;

  @BeforeEach
  public void beforeEach() {
    workflowLogRepository.deleteAll();
    mediatedRequestsRepository.deleteAll();
  }

  @Test
  @SneakyThrows
  void mediatedRequestConfirmationForLocalInstanceAndItem() {
    MediatedRequestEntity initialRequest = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION));

    UUID circulationRequestId = UUID.randomUUID();
    UUID instanceId = initialRequest.getInstanceId();

    ConsortiumItems consortiumItems = new ConsortiumItems()
      .items(List.of(new ConsortiumItem()
        .id("9428231b-dd31-4f70-8406-fe22fbdeabc2")
        .tenantId(TENANT_ID_CONSORTIUM)));

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_ITEMS_URL))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("tenantId", equalTo(TENANT_ID_CONSORTIUM))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL))
      .willReturn(jsonResponse(consortiumItems, HttpStatus.SC_OK)));

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
    wireMockServer.verify(getRequestedFor(urlPathMatching(SEARCH_ITEMS_URL))
        .withQueryParam("instanceId", equalTo(instanceId.toString()))
        .withQueryParam("tenantId", equalTo(TENANT_ID_CONSORTIUM))
        .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(postRequestedFor(urlMatching(CIRCULATION_REQUESTS_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(ECS_TLR_URL)));
  }

  @Test
  @SneakyThrows
  void mediatedRequestConfirmationForLocalInstanceAndRemoteItem() {
    MediatedRequestEntity initialRequest = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION));
    UUID instanceId = initialRequest.getInstanceId();

    wireMockServer.stubFor(WireMock.get(urlMatching(ITEMS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new Items().items(emptyList()), HttpStatus.SC_OK)));

    UUID primaryRequestId = UUID.randomUUID();
    EcsTlr ecsTlr = new EcsTlr().id(randomId())
      .primaryRequestId(primaryRequestId.toString());

    wireMockServer.stubFor(WireMock.post(urlMatching(ECS_TLR_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL))
      .willReturn(jsonResponse(ecsTlr, HttpStatus.SC_OK)));

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_ITEMS_URL))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("tenantId", equalTo(TENANT_ID_CONSORTIUM))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL))
      .willReturn(jsonResponse(new ConsortiumItems().items(new ArrayList<>()), HttpStatus.SC_OK)));

    confirmMediatedRequest(initialRequest.getId())
      .andExpect(status().isNoContent());

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(initialRequest.getId())
      .orElseThrow();
    assertThat(updatedRequest.getConfirmedRequestId(), is(primaryRequestId));

    wireMockServer.verify(getRequestedFor(urlMatching(INSTANCES_URL + "/" + instanceId))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(getRequestedFor(urlPathMatching(SEARCH_ITEMS_URL))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("tenantId", equalTo(TENANT_ID_CONSORTIUM))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CENTRAL)));
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
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(SEARCH_ITEMS_URL)));
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
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(SEARCH_ITEMS_URL)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(CIRCULATION_REQUESTS_URL)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(ECS_TLR_URL)));
  }

  @Test
  @SneakyThrows
  void successfulMediatedRequestDecline() {
    // given
    MediatedRequestEntity initialRequest = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(NEW_AWAITING_CONFIRMATION));

    // when
    declineMediatedRequest(initialRequest.getId())
      .andExpect(status().isNoContent());

    // then
    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(initialRequest.getId())
      .orElseThrow();
    assertEquals(updatedRequest.getMediatedRequestStatus(), MediatedRequestStatus.CLOSED);
    assertEquals(updatedRequest.getStatus(), MediatedRequest.StatusEnum.CLOSED_DECLINED.getValue());
    assertEquals(updatedRequest.getMediatedRequestStep(), MediatedRequestStep.DECLINED.getValue());
  }

  @Test
  @SneakyThrows
  void declineRequestConfirmationFailsForNonExistentRequest() {
    // given
    UUID mediatedRequestId = UUID.randomUUID();

    // when - then
    declineMediatedRequest(mediatedRequestId)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request was not found: " + mediatedRequestId)));

    wireMockServer.verify(0, getRequestedFor(urlMatching(INSTANCES_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(SEARCH_ITEMS_URL)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(CIRCULATION_REQUESTS_URL)));
    wireMockServer.verify(0, postRequestedFor(urlMatching(ECS_TLR_URL)));
  }

  @SneakyThrows
  private ResultActions declineMediatedRequest(UUID mediatedRequestId) {
    return mockMvc.perform(
      post(format(DECLINE_MEDIATED_REQUEST_URL_TEMPLATE, mediatedRequestId))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @SneakyThrows
  void successfulItemArrivalConfirmation() {
    MediatedRequestEntity request = createMediatedRequestEntity();

    confirmItemArrival("A14837334314", request)
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

    wireMockServer.verify(1, getRequestedFor(urlPathMatching(SEARCH_INSTANCES_URL))
      .withQueryParam("query", equalTo("id==" + request.getInstanceId()))
      .withQueryParam("expandAll", equalTo("true"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(ITEMS_URL + "/" + request.getItemId()))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE)));

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(request.getId())
      .orElseThrow();
    assertThat(updatedRequest.getMediatedRequestStep(), is("Item arrived"));
    assertThat(updatedRequest.getStatus(), is("Open - Item arrived"));
  }

  @Test
  @SneakyThrows
  void itemArrivalConfirmationFailsWhenMediatedRequestIsNotFoundByItemBarcode() {
    confirmItemArrival("random-barcode", new MediatedRequestEntity()
      .withInstanceId(UUID.randomUUID())
      .withItemId(UUID.randomUUID()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Mediated request for arrival confirmation of item with barcode 'random-barcode' was not found")));
  }

  @Test
  @SneakyThrows
  void itemArrivalConfirmationFailsWhenMediatedRequestIsNotFoundByStatus() {
    MediatedRequestEntity mediatedRequest = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(OPEN_ITEM_ARRIVED));// wrong status

    confirmItemArrival("A14837334314", mediatedRequest)
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
  private ResultActions confirmItemArrival(String itemBarcode, MediatedRequestEntity request) {
    var instanceId = request.getInstanceId().toString();
    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new SearchInstancesResponse().addInstancesItem(
          new SearchInstance()
            .id(instanceId)
            .tenantId(TENANT_ID_CONSORTIUM)
            .addItemsItem(new SearchItem()
              .id(request.getItemId().toString())
              .tenantId(TENANT_ID_COLLEGE))),
        HttpStatus.SC_OK)));

    return mockMvc.perform(
      post(CONFIRM_ITEM_ARRIVAL_URL)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(new ConfirmItemArrivalRequest().itemBarcode(itemBarcode))));
  }

  @Test
  @SneakyThrows
  void sendItemInTransitSuccess() {
    MediatedRequestEntity request = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
    );

    ResultActions resultActions = sendItemInTransit("A14837334314", request)
      .andExpect(status().isOk())
      .andExpect(jsonPath("inTransitDate", notNullValue()))
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
      .andExpect(jsonPath("mediatedRequest.status", is("Open - In transit to be checked out")))
      .andExpect(jsonPath("requester.id", is("9812e24b-0a66-457a-832c-c5e789797e35")))
      .andExpect(jsonPath("requester.barcode", is("111")))
      .andExpect(jsonPath("requester.firstName", is("Requester")))
      .andExpect(jsonPath("requester.middleName", is("X")))
      .andExpect(jsonPath("requester.lastName", is("Mediated")));

    expectStaffSlipContext(resultActions);

    MediatedRequestEntity updatedRequest = mediatedRequestsRepository.findById(request.getId())
      .orElseThrow();
    assertThat(updatedRequest.getMediatedRequestStep(), is("In transit to be checked out"));
    assertThat(updatedRequest.getStatus(), is("Open - In transit to be checked out"));
  }

  @SneakyThrows
  private static void expectStaffSlipContext(ResultActions resultActions) {
    resultActions
      .andExpect(jsonPath("staffSlipContext.item.title", is("ABA Journal")))
      .andExpect(jsonPath("staffSlipContext.item.primaryContributor", is("First, Author")))
      .andExpect(jsonPath("staffSlipContext.item.allContributors", is("First, Author; Second, Writer")))
      .andExpect(jsonPath("staffSlipContext.item.barcode", is("A14837334314")))
      .andExpect(jsonPath("staffSlipContext.item.status", is("Available")))
      .andExpect(jsonPath("staffSlipContext.item.enumeration", is("v.70:no.7-12")))
      .andExpect(jsonPath("staffSlipContext.item.volume", is("vol.1")))
      .andExpect(jsonPath("staffSlipContext.item.chronology", is("1984:July-Dec.")))
      .andExpect(jsonPath("staffSlipContext.item.copy", is("cp.1")))
      .andExpect(jsonPath("staffSlipContext.item.displaySummary", is("test summary")))
      .andExpect(jsonPath("staffSlipContext.item.yearCaption", is("")))
      .andExpect(jsonPath("staffSlipContext.item.status", is("Available")))
      .andExpect(jsonPath("staffSlipContext.item.materialType", is("unspecified")))
      .andExpect(jsonPath("staffSlipContext.item.loanType", is("Reading room")))
      .andExpect(jsonPath("staffSlipContext.item.effectiveLocationSpecific", is("Main Library")))
      .andExpect(jsonPath("staffSlipContext.item.effectiveLocationLibrary", is("Datalogisk Institut")))
      .andExpect(jsonPath("staffSlipContext.item.effectiveLocationCampus", is("City Campus")))
      .andExpect(jsonPath("staffSlipContext.item.effectiveLocationInstitution", is("Københavns Universitet")))
      .andExpect(jsonPath("staffSlipContext.item.effectiveLocationPrimaryServicePointName", is("Circ Desk 1")))
      .andExpect(jsonPath("staffSlipContext.item.callNumber", is("CN")))
      .andExpect(jsonPath("staffSlipContext.item.callNumberPrefix", is("PFX")))
      .andExpect(jsonPath("staffSlipContext.item.callNumberSuffix", is("SFX")))
      .andExpect(jsonPath("staffSlipContext.item.fromServicePoint", is("Circ Desk 1")))
      .andExpect(jsonPath("staffSlipContext.item.toServicePoint", is("Circ Desk 1")));
  }

  @Test
  @SneakyThrows
  void sendItemInTransitItemNotFound() {
    MediatedRequestEntity request = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(OPEN_ITEM_ARRIVED).withItemId(UUID.fromString(NOT_FOUND_ITEM_UUID))
    );

    sendItemInTransit("A14837334314", request).andExpect(status().isNotFound());
  }

  @SneakyThrows
  @ParameterizedTest
  @NullAndEmptySource
  void sendItemInTransitShouldReturnNotFoundIfNoSearchInstancesFound(List<SearchInstance> instances) {
    var request = mediatedRequestsRepository.save(buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
      .withItemId(UUID.fromString(NOT_FOUND_ITEM_UUID)));

    var instanceId = request.getInstanceId().toString();
    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new SearchInstancesResponse().instances(instances), HttpStatus.SC_OK)));

    mockMvc.perform(
      post(SEND_ITEM_IN_TRANSIT_URL)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(new SendItemInTransitRequest()
          .itemBarcode(request.getItemBarcode()))));

    sendItemInTransit("A14837334314", request).andExpect(status().isNotFound());
  }

  @SneakyThrows
  @ParameterizedTest
  @NullAndEmptySource
  void sendItemInTransitShouldReturnNotFoundIfNoSearchItemsFound(List<SearchItem> items) {
    var request = mediatedRequestsRepository.save(buildMediatedRequestEntity(OPEN_ITEM_ARRIVED)
      .withItemId(UUID.fromString(NOT_FOUND_ITEM_UUID)));

    var instanceId = request.getInstanceId().toString();
    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new SearchInstancesResponse().instances(
        List.of(new SearchInstance().id(instanceId)
          .tenantId(TENANT_ID_CONSORTIUM)
          .items(items))), HttpStatus.SC_OK)));

    mockMvc.perform(
      post(SEND_ITEM_IN_TRANSIT_URL)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(new SendItemInTransitRequest()
          .itemBarcode(request.getItemBarcode()))));

    sendItemInTransit("A14837334314", request).andExpect(status().isNotFound());
  }

  @Test
  @SneakyThrows
  void sendItemInTransitFailsWhenMediatedRequestIsNotFoundByItemBarcode() {
    sendItemInTransit("random-barcode", new MediatedRequestEntity()
      .withInstanceId(UUID.randomUUID())
      .withItemId(UUID.randomUUID()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Send item in transit: mediated request for item 'random-barcode' was not found")));
  }

  @Test
  @SneakyThrows
  void sendItemInTransitFailsWhenMediatedRequestIsNotFoundByStatus() {
    MediatedRequestEntity request = mediatedRequestsRepository.save(
      buildMediatedRequestEntity(OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT));// wrong status

    sendItemInTransit("A14837334314", request)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("errors").value(iterableWithSize(1)))
      .andExpect(jsonPath("errors[0].type").value("EntityNotFoundException"))
      .andExpect(jsonPath("errors[0].message")
        .value(is("Send item in transit: mediated request for item 'A14837334314' was not found")));
  }

  @SneakyThrows
  private ResultActions sendItemInTransit(String itemBarcode, MediatedRequestEntity request) {
    var instanceId = request.getInstanceId().toString();
    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new SearchInstancesResponse().addInstancesItem(
          new SearchInstance()
            .id(instanceId)
            .tenantId(TENANT_ID_CONSORTIUM)
            .addItemsItem(new SearchItem()
              .id(request.getItemId().toString())
              .tenantId(TENANT_ID_COLLEGE))),
        HttpStatus.SC_OK)));

    return mockMvc.perform(
      post(SEND_ITEM_IN_TRANSIT_URL)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(new SendItemInTransitRequest().itemBarcode(itemBarcode))));
  }

  @SneakyThrows
  private ResultActions confirmMediatedRequest(UUID mediatedRequestId) {
    return mockMvc.perform(
      post(format(CONFIRM_MEDIATED_REQUEST_URL_TEMPLATE, mediatedRequestId))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }

}
