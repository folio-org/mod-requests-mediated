package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.folio.mr.domain.dto.MediatedRequest.FulfillmentPreferenceEnum.DELIVERY;
import static org.folio.mr.domain.dto.MediatedRequest.FulfillmentPreferenceEnum.HOLD_SHELF;
import static org.folio.mr.domain.dto.MediatedRequest.RequestLevelEnum.ITEM;
import static org.folio.mr.domain.dto.MediatedRequest.RequestLevelEnum.TITLE;
import static org.folio.mr.domain.dto.MediatedRequest.RequestTypeEnum.HOLD;
import static org.folio.mr.domain.dto.MediatedRequest.RequestTypeEnum.PAGE;
import static org.folio.mr.util.TestUtils.dateToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.RequestLevel;
import org.folio.mr.domain.RequestType;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.dto.SearchItem;
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
class MediatedRequestsApiTest extends BaseIT {

  private static final String UUID_PATTEN =
    "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$";
  private static final String URL_MEDIATED_REQUESTS = "/requests-mediated/mediated-requests";

  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @BeforeEach
  public void clearDatabase() {
    mediatedRequestsRepository.deleteAll();
  }

  @Test
  void getByIdNotFound() throws Exception {
    getRequestById(randomId())
      .andExpect(status().isNotFound());
  }

  @Test
  void getReturnsEmptyArrayWhenNoRequestsAreFound() throws Exception {
    getAllRequests()
      .andExpect(status().isOk())
      .andExpect(jsonPath("mediatedRequests", emptyIterable()))
      .andExpect(jsonPath("totalRecords", is(0)));
  }

  @Test
  void mediatedRequestWithAllDetailsShouldBeCreated() {
    MediatedRequest mediatedRequest = createMediatedRequest();
    MediatedRequestEntity entity = mediatedRequestsRepository.findById(UUID.fromString(mediatedRequest.getId()))
      .orElseThrow(() -> new AssertionError("Failed to find mediated request in DB"));

    assertThat(entity.getId().toString(), is(mediatedRequest.getId()));
    assertThat(entity.getRequestLevel(), is(RequestLevel.ITEM));
    assertThat(entity.getRequestType(), is(RequestType.PAGE));
    assertThat(entity.getRequestDate().getTime(), is(mediatedRequest.getRequestDate().getTime()));
    assertThat(entity.getPatronComments(), is("test"));
    assertThat(entity.getRequesterId(), is(UUID.fromString("9812e24b-0a66-457a-832c-c5e789797e35")));
    assertThat(entity.getRequesterFirstName(), is("Requester"));
    assertThat(entity.getRequesterMiddleName(), is("X"));
    assertThat(entity.getRequesterLastName(), is("Mediated"));
    assertThat(entity.getRequesterBarcode(), is("111"));
    assertThat(entity.getProxyUserId(), is(UUID.fromString("7b89ee8c-6524-432e-8c57-82bc860af38f")));
    assertThat(entity.getProxyFirstName(), is("User"));
    assertThat(entity.getProxyMiddleName(), is("M"));
    assertThat(entity.getProxyLastName(), is("Proxy"));
    assertThat(entity.getProxyBarcode(), is("proxy"));
    assertThat(entity.getInstanceId(), is(UUID.fromString("69640328-788e-43fc-9c3c-af39e243f3b7")));
    assertThat(entity.getHoldingsRecordId(), is(UUID.fromString("0c45bb50-7c9b-48b0-86eb-178a494e25fe")));
    assertThat(entity.getItemId(), is(UUID.fromString("9428231b-dd31-4f70-8406-fe22fbdeabc2")));
    assertThat(entity.getItemBarcode(), is("A14837334314"));
    assertThat(entity.getMediatedWorkflow(), is("Private request"));
    assertThat(entity.getMediatedRequestStatus(), is(MediatedRequestStatus.NEW));
    assertThat(entity.getMediatedRequestStep(), is("Awaiting confirmation"));
    assertThat(entity.getStatus(), is("New - Awaiting confirmation"));
    assertThat(entity.getCancellationReasonId(), nullValue());
    assertThat(entity.getCancelledDate(), nullValue());
    assertThat(entity.getCancelledByUserId(), nullValue());
    assertThat(entity.getCancellationAdditionalInformation(), nullValue());
    assertThat(entity.getPosition(), nullValue());
    assertThat(entity.getDeliveryAddressTypeId(), nullValue());
    assertThat(entity.getPickupServicePointId(), is(UUID.fromString("3a40852d-49fd-4df2-a1f9-6e2641a6e91f")));
    assertThat(entity.getConfirmedRequestId(), nullValue());
    assertThat(entity.getCallNumber(), is("CN"));
    assertThat(entity.getCallNumberPrefix(), is("PFX"));
    assertThat(entity.getCallNumberSuffix(), is("SFX"));
    assertThat(entity.getFullCallNumber(), is("PFX CN SFX"));
    assertThat(entity.getShelvingOrder(), is("CN vol.1 v.70:no.7-12 1984:July-Dec. cp.1 SFX"));
    assertThat(entity.getPickupServicePointName(), is("Circ Desk 1"));
    assertThat(entity.getCreatedDate(), notNullValue());
    assertThat(entity.getUpdatedDate(), notNullValue());
    assertThat(entity.getCreatedByUserId(), is(UUID.fromString(USER_ID)));
    assertThat(entity.getUpdatedByUserId(), is(UUID.fromString(USER_ID)));
    assertThat(entity.getCreatedByUsername(), nullValue());
    assertThat(entity.getUpdatedByUsername(), nullValue());
  }

  @Test
  @SneakyThrows
  void mediatedRequestWithoutSomeDetailsShouldBeCreated() {
    Date requestDate = new Date();
    String instanceId = "69640328-788e-43fc-9c3c-af39e243f3b7";
    MediatedRequest request = new MediatedRequest()
      .requestLevel(TITLE)
      .requestType(HOLD)
      .fulfillmentPreference(DELIVERY)
      .deliveryAddressTypeId("93d3d88d-499b-45d0-9bc7-ac73c3a19880")
      .requestDate(requestDate)
      .requesterId("9812e24b-0a66-457a-832c-c5e789797e35")
      .instanceId(instanceId);

    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances" + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new SearchInstancesResponse().addInstancesItem(
        new SearchInstance()
          .id(instanceId)
          .tenantId(TENANT_ID_CONSORTIUM)),
        HttpStatus.SC_OK)));

    String responseBody = postRequest(request)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("id", matchesPattern(UUID_PATTEN)))
      .andExpect(jsonPath("requestLevel", is("Title")))
      .andExpect(jsonPath("requestType", is("Hold")))
      .andExpect(jsonPath("requestDate", is(dateToString(requestDate))))
      .andExpect(jsonPath("patronComments").doesNotExist())
      .andExpect(jsonPath("requesterId", is("9812e24b-0a66-457a-832c-c5e789797e35")))
      .andExpect(jsonPath("proxyUserId").doesNotExist())
      .andExpect(jsonPath("instanceId", is("69640328-788e-43fc-9c3c-af39e243f3b7")))
      .andExpect(jsonPath("holdingsRecordId").doesNotExist())
      .andExpect(jsonPath("itemId").doesNotExist())
      .andExpect(jsonPath("mediatedRequestStatus", is("New")))
      .andExpect(jsonPath("mediatedRequestStep", is("Awaiting confirmation")))
      .andExpect(jsonPath("mediatedWorkflow", is("Private request")))
      .andExpect(jsonPath("status", is("New - Awaiting confirmation")))
      .andExpect(jsonPath("cancellationReasonId").doesNotExist())
      .andExpect(jsonPath("cancelledByUserId").doesNotExist())
      .andExpect(jsonPath("cancellationAdditionalInformation").doesNotExist())
      .andExpect(jsonPath("cancelledDate").doesNotExist())
      .andExpect(jsonPath("position").doesNotExist())
      .andExpect(jsonPath("fulfillmentPreference", is("Delivery")))
      .andExpect(jsonPath("deliveryAddressTypeId", is("93d3d88d-499b-45d0-9bc7-ac73c3a19880")))
      .andExpect(jsonPath("confirmedRequestId").doesNotExist())
      .andExpect(jsonPath("pickupServicePointId").doesNotExist())
      .andExpect(jsonPath("instance.title", is("ABA Journal")))
      .andExpect(jsonPath("instance.identifiers", hasSize(2)))
      .andExpect(jsonPath("instance.identifiers[0].value", is("0747-0088")))
      .andExpect(jsonPath("instance.identifiers[0].identifierTypeId", is("913300b2-03ed-469a-8179-c1092c991227")))
      .andExpect(jsonPath("instance.identifiers[1].value", is("84641839")))
      .andExpect(jsonPath("instance.identifiers[1].identifierTypeId", is("c858e4f2-2b6b-4385-842b-60732ee14abb")))
      .andExpect(jsonPath("instance.publication", hasSize(2)))
      .andExpect(jsonPath("instance.publication[0].publisher", is("American Bar Association")))
      .andExpect(jsonPath("instance.publication[0].place", is("Chicago, Ill.")))
      .andExpect(jsonPath("instance.publication[0].dateOfPublication", is("1915-1983")))
      .andExpect(jsonPath("instance.publication[0].role", is("role1")))
      .andExpect(jsonPath("instance.publication[1].publisher", is("Penguin")))
      .andExpect(jsonPath("instance.publication[1].place", is("Boston, Mass.")))
      .andExpect(jsonPath("instance.publication[1].dateOfPublication", is("1916-1975")))
      .andExpect(jsonPath("instance.publication[1].role", is("role2")))
      .andExpect(jsonPath("instance.editions", contains("ed.1", "ed.2")))
      .andExpect(jsonPath("instance.contributorNames", hasSize(2)))
      .andExpect(jsonPath("instance.contributorNames[0].name", is("First, Author")))
      .andExpect(jsonPath("instance.contributorNames[1].name", is("Second, Writer")))
      .andExpect(jsonPath("instance.hrid", is("inst000000000001")))
      .andExpect(jsonPath("item").doesNotExist())
      .andExpect(jsonPath("staffSlipContext").doesNotExist())
      .andExpect(jsonPath("requester.barcode", is("111")))
      .andExpect(jsonPath("requester.firstName", is("Requester")))
      .andExpect(jsonPath("requester.middleName", is("X")))
      .andExpect(jsonPath("requester.lastName", is("Mediated")))
      .andExpect(jsonPath("requester.patronGroupId", is("3684a786-6671-4268-8ed0-9db82ebca60b")))
      .andExpect(jsonPath("requester.patronGroup.id", is("3684a786-6671-4268-8ed0-9db82ebca60b")))
      .andExpect(jsonPath("requester.patronGroup.group", is("staff")))
      .andExpect(jsonPath("requester.patronGroup.desc", is("Staff Member")))
      .andExpect(jsonPath("deliveryAddress.addressTypeId", is("93d3d88d-499b-45d0-9bc7-ac73c3a19880")))
      .andExpect(jsonPath("deliveryAddress.addressLine1", is("2311 North")))
      .andExpect(jsonPath("deliveryAddress.addressLine2", is("Los Robles Avenue")))
      .andExpect(jsonPath("deliveryAddress.city", is("Pasadena")))
      .andExpect(jsonPath("deliveryAddress.region", is("California")))
      .andExpect(jsonPath("deliveryAddress.countryId", is("US")))
      .andExpect(jsonPath("deliveryAddress.postalCode", is("91101")))
      .andExpect(jsonPath("proxy").doesNotExist())
      .andExpect(jsonPath("pickupServicePoint").doesNotExist())
      .andExpect(jsonPath("searchIndex").doesNotExist())
      .andExpect(jsonPath("metadata.createdDate").exists())
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUsername").doesNotExist())
      .andExpect(jsonPath("metadata.updatedDate").exists())
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.updatedByUsername").doesNotExist())
      .andReturn().getResponse().getContentAsString();

    MediatedRequest mediatedRequest = OBJECT_MAPPER.readValue(responseBody, MediatedRequest.class);

    MediatedRequestEntity entity = mediatedRequestsRepository.findById(
      UUID.fromString(mediatedRequest.getId()))
      .orElseThrow(() -> new AssertionError("Failed to find mediated request in DB"));

    assertThat(entity.getId().toString(), is(mediatedRequest.getId()));
    assertThat(entity.getRequestLevel(), is(RequestLevel.TITLE));
    assertThat(entity.getRequestType(), is(RequestType.HOLD));
    assertThat(entity.getRequestDate().getTime(), is(mediatedRequest.getRequestDate().getTime()));
    assertThat(entity.getPatronComments(), nullValue());
    assertThat(entity.getRequesterId(), is(UUID.fromString("9812e24b-0a66-457a-832c-c5e789797e35")));
    assertThat(entity.getRequesterFirstName(), is("Requester"));
    assertThat(entity.getRequesterMiddleName(), is("X"));
    assertThat(entity.getRequesterLastName(), is("Mediated"));
    assertThat(entity.getRequesterBarcode(), is("111"));
    assertThat(entity.getProxyUserId(), nullValue());
    assertThat(entity.getProxyFirstName(), nullValue());
    assertThat(entity.getProxyMiddleName(), nullValue());
    assertThat(entity.getProxyLastName(), nullValue());
    assertThat(entity.getProxyBarcode(), nullValue());
    assertThat(entity.getInstanceId(), is(UUID.fromString("69640328-788e-43fc-9c3c-af39e243f3b7")));
    assertThat(entity.getHoldingsRecordId(), nullValue());
    assertThat(entity.getItemId(), nullValue());
    assertThat(entity.getItemBarcode(), nullValue());
    assertThat(entity.getMediatedWorkflow(), is("Private request"));
    assertThat(entity.getMediatedRequestStatus(), is(MediatedRequestStatus.NEW));
    assertThat(entity.getMediatedRequestStep(), is("Awaiting confirmation"));
    assertThat(entity.getStatus(), is("New - Awaiting confirmation"));
    assertThat(entity.getCancellationReasonId(), nullValue());
    assertThat(entity.getCancelledDate(), nullValue());
    assertThat(entity.getCancelledByUserId(), nullValue());
    assertThat(entity.getCancellationAdditionalInformation(), nullValue());
    assertThat(entity.getPosition(), nullValue());
    assertThat(entity.getDeliveryAddressTypeId(), is(UUID.fromString("93d3d88d-499b-45d0-9bc7-ac73c3a19880")));
    assertThat(entity.getPickupServicePointId(), nullValue());
    assertThat(entity.getConfirmedRequestId(), nullValue());
    assertThat(entity.getCallNumber(), nullValue());
    assertThat(entity.getCallNumberPrefix(), nullValue());
    assertThat(entity.getCallNumberSuffix(), nullValue());
    assertThat(entity.getFullCallNumber(), nullValue());
    assertThat(entity.getShelvingOrder(), nullValue());
    assertThat(entity.getPickupServicePointName(), nullValue());
    assertThat(entity.getCreatedDate(), notNullValue());
    assertThat(entity.getUpdatedDate(), notNullValue());
    assertThat(entity.getCreatedByUserId(), is(UUID.fromString(USER_ID)));
    assertThat(entity.getUpdatedByUserId(), is(UUID.fromString(USER_ID)));
    assertThat(entity.getCreatedByUsername(), nullValue());
    assertThat(entity.getUpdatedByUsername(), nullValue());
  }

  @Test
  @SneakyThrows
  void minimalMediatedRequestShouldBeCreated() {
    Date requestDate = new Date();
    String instanceId = "69640328-788e-43fc-9c3c-af39e243f3b7";
    MediatedRequest request = new MediatedRequest()
      .requestLevel(TITLE)
      .requestDate(requestDate)
      .requesterId("9812e24b-0a66-457a-832c-c5e789797e35")
      .instanceId(instanceId);

    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances" + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new SearchInstancesResponse().addInstancesItem(
          new SearchInstance()
            .id(instanceId)
            .tenantId(TENANT_ID_CONSORTIUM)),
        HttpStatus.SC_OK)));

    postRequest(request).andExpect(status().isCreated());
  }

  @SneakyThrows
  @Test
  void mediatedRequestsShouldBeRetrieved() {
    getAllRequests()
      .andExpect(status().isOk());
  }

  @SneakyThrows
  @Test
  void mediatedRequestsShouldBeRetrievedByQuery() {
    MediatedRequest mediatedRequest = createMediatedRequest();
    getRequestsByQuery("status==(\"New - Awaiting confirmation\" or \"Closed - Filled\")")
      .andExpect(status().isOk())
      .andExpect(jsonPath("mediatedRequests", iterableWithSize(1)))
      .andExpect(jsonPath("mediatedRequests[0].id", is(mediatedRequest.getId())));
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeCreatedAndRetrievedById() {
    MediatedRequest mediatedRequest = createMediatedRequest();
    getRequestById(mediatedRequest.getId())
      .andExpect(status().isOk())
      .andExpect(jsonPath("id", is(mediatedRequest.getId())));
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeCreatedAndRetrievedByFullCallNumberQuery() {
    MediatedRequest mediatedRequest = createMediatedRequest();
    getRequestsByQuery("fullCallNumber==\"*X CN S*\"")
      .andExpect(status().isOk())
      .andExpect(jsonPath("mediatedRequests", iterableWithSize(1)))
      .andExpect(jsonPath("mediatedRequests[0].id", is(mediatedRequest.getId())));
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeUpdated() {
    MediatedRequest mediatedRequest = createMediatedRequest();
    mediatedRequest.setStatus(MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP);

    putRequest(mediatedRequest)
      .andExpect(status().isNoContent());

    getRequestById(mediatedRequest.getId())
      .andExpect(status().isOk())
      .andExpect(jsonPath("status", is("Open - Awaiting pickup")))
      .andExpect(jsonPath("mediatedRequestStatus", is("Open")))
      .andExpect(jsonPath("mediatedRequestStep", is("Awaiting pickup")));
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldNotBeUpdatedWhenNotFound() {
    MediatedRequest mediatedRequest = createMediatedRequest();
    deleteRequest(mediatedRequest.getId())
      .andExpect(status().isNoContent());

    putRequest(mediatedRequest)
      .andExpect(status().isNotFound());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldBeCreatedAndDeleted() {
    MediatedRequest mediatedRequest = createMediatedRequest();
    deleteRequest(mediatedRequest.getId())
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  @Test
  void mediatedRequestShouldNotBeFoundForDelete() {
    deleteRequest(randomId())
      .andExpect(status().isNotFound());
  }

  @SneakyThrows
  private MediatedRequest createMediatedRequest() {
    Date requestDate = new Date();
    String instanceId = "69640328-788e-43fc-9c3c-af39e243f3b7";
    MediatedRequest request = new MediatedRequest()
      .requestLevel(ITEM)
      .requestType(PAGE)
      .fulfillmentPreference(HOLD_SHELF)
      .requestDate(requestDate)
      .patronComments("test")
      .requesterId("9812e24b-0a66-457a-832c-c5e789797e35")
      .proxyUserId("7b89ee8c-6524-432e-8c57-82bc860af38f")
      .instanceId(instanceId)
      .holdingsRecordId("0c45bb50-7c9b-48b0-86eb-178a494e25fe")
      .itemId("9428231b-dd31-4f70-8406-fe22fbdeabc2")
      .pickupServicePointId("3a40852d-49fd-4df2-a1f9-6e2641a6e91f");

    String itemId = "9428231b-dd31-4f70-8406-fe22fbdeabc2";
    SearchItem searchItem = new SearchItem()
      .id(itemId)
      .tenantId(TENANT_ID_CONSORTIUM);

    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances" + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new SearchInstancesResponse().addInstancesItem(
          new SearchInstance()
            .id(instanceId)
            .tenantId(TENANT_ID_CONSORTIUM)
            .addItemsItem(searchItem)),
        HttpStatus.SC_OK)));

    String responseBody = postRequest(request)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("id", matchesPattern(UUID_PATTEN)))
      .andExpect(jsonPath("requestLevel", is("Item")))
      .andExpect(jsonPath("requestType", is("Page")))
      .andExpect(jsonPath("requestDate", is(dateToString(requestDate))))
      .andExpect(jsonPath("patronComments", is("test")))
      .andExpect(jsonPath("requesterId", is("9812e24b-0a66-457a-832c-c5e789797e35")))
      .andExpect(jsonPath("proxyUserId", is("7b89ee8c-6524-432e-8c57-82bc860af38f")))
      .andExpect(jsonPath("instanceId", is(instanceId)))
      .andExpect(jsonPath("holdingsRecordId", is("0c45bb50-7c9b-48b0-86eb-178a494e25fe")))
      .andExpect(jsonPath("itemId", is(itemId)))
      .andExpect(jsonPath("mediatedRequestStatus", is("New")))
      .andExpect(jsonPath("mediatedRequestStep", is("Awaiting confirmation")))
      .andExpect(jsonPath("mediatedWorkflow", is("Private request")))
      .andExpect(jsonPath("status", is("New - Awaiting confirmation")))
      .andExpect(jsonPath("cancellationReasonId").doesNotExist())
      .andExpect(jsonPath("cancelledByUserId").doesNotExist())
      .andExpect(jsonPath("cancellationAdditionalInformation").doesNotExist())
      .andExpect(jsonPath("cancelledDate").doesNotExist())
      .andExpect(jsonPath("position").doesNotExist())
      .andExpect(jsonPath("fulfillmentPreference", is("Hold Shelf")))
      .andExpect(jsonPath("deliveryAddressTypeId").doesNotExist())
      .andExpect(jsonPath("deliveryAddress").doesNotExist())
      .andExpect(jsonPath("confirmedRequestId").doesNotExist())
      .andExpect(jsonPath("pickupServicePointId", is("3a40852d-49fd-4df2-a1f9-6e2641a6e91f")))
      .andExpect(jsonPath("instance.title", is("ABA Journal")))
      .andExpect(jsonPath("instance.identifiers", hasSize(2)))
      .andExpect(jsonPath("instance.identifiers[0].value", is("0747-0088")))
      .andExpect(jsonPath("instance.identifiers[0].identifierTypeId", is("913300b2-03ed-469a-8179-c1092c991227")))
      .andExpect(jsonPath("instance.identifiers[1].value", is("84641839")))
      .andExpect(jsonPath("instance.identifiers[1].identifierTypeId", is("c858e4f2-2b6b-4385-842b-60732ee14abb")))
      .andExpect(jsonPath("instance.publication", hasSize(2)))
      .andExpect(jsonPath("instance.publication[0].publisher", is("American Bar Association")))
      .andExpect(jsonPath("instance.publication[0].place", is("Chicago, Ill.")))
      .andExpect(jsonPath("instance.publication[0].dateOfPublication", is("1915-1983")))
      .andExpect(jsonPath("instance.publication[0].role", is("role1")))
      .andExpect(jsonPath("instance.publication[1].publisher", is("Penguin")))
      .andExpect(jsonPath("instance.publication[1].place", is("Boston, Mass.")))
      .andExpect(jsonPath("instance.publication[1].dateOfPublication", is("1916-1975")))
      .andExpect(jsonPath("instance.publication[1].role", is("role2")))
      .andExpect(jsonPath("instance.editions", contains("ed.1", "ed.2")))
      .andExpect(jsonPath("instance.contributorNames", hasSize(2)))
      .andExpect(jsonPath("instance.contributorNames[0].name", is("First, Author")))
      .andExpect(jsonPath("instance.contributorNames[1].name", is("Second, Writer")))
      .andExpect(jsonPath("item.barcode", is("A14837334314")))
      .andExpect(jsonPath("item.enumeration", is("v.70:no.7-12")))
      .andExpect(jsonPath("item.volume", is("vol.1")))
      .andExpect(jsonPath("item.chronology", is("1984:July-Dec.")))
      .andExpect(jsonPath("item.displaySummary", is("test summary")))
      .andExpect(jsonPath("item.status", is("Available")))
      .andExpect(jsonPath("item.copyNumber", is("cp.1")))
      .andExpect(jsonPath("item.location.name", is("Main Library")))
      .andExpect(jsonPath("item.location.code", is("KU/CC/DI/M")))
      .andExpect(jsonPath("item.location.libraryName", is("Datalogisk Institut")))
      .andExpect(jsonPath("item.callNumber", is("CN")))
      .andExpect(jsonPath("item.callNumberComponents.callNumber", is("CN")))
      .andExpect(jsonPath("item.callNumberComponents.prefix", is("PFX")))
      .andExpect(jsonPath("item.callNumberComponents.suffix", is("SFX")))
      .andExpect(jsonPath("requester.barcode", is("111")))
      .andExpect(jsonPath("requester.firstName", is("Requester")))
      .andExpect(jsonPath("requester.middleName", is("X")))
      .andExpect(jsonPath("requester.lastName", is("Mediated")))
      .andExpect(jsonPath("requester.patronGroupId", is("3684a786-6671-4268-8ed0-9db82ebca60b")))
      .andExpect(jsonPath("requester.patronGroup.id", is("3684a786-6671-4268-8ed0-9db82ebca60b")))
      .andExpect(jsonPath("requester.patronGroup.group", is("staff")))
      .andExpect(jsonPath("requester.patronGroup.desc", is("Staff Member")))
      .andExpect(jsonPath("proxy.barcode", is("proxy")))
      .andExpect(jsonPath("proxy.firstName", is("User")))
      .andExpect(jsonPath("proxy.middleName", is("M")))
      .andExpect(jsonPath("proxy.lastName", is("Proxy")))
      .andExpect(jsonPath("proxy.patronGroupId", is("503a81cd-6c26-400f-b620-14c08943697c")))
      .andExpect(jsonPath("proxy.patronGroup.id", is("503a81cd-6c26-400f-b620-14c08943697c")))
      .andExpect(jsonPath("proxy.patronGroup.group", is("faculty")))
      .andExpect(jsonPath("proxy.patronGroup.desc", is("Faculty Member")))
      .andExpect(jsonPath("pickupServicePoint.name", is("Circ Desk 1")))
      .andExpect(jsonPath("pickupServicePoint.code", is("cd1")))
      .andExpect(jsonPath("pickupServicePoint.discoveryDisplayName", is("Circulation Desk -- Hallway")))
      .andExpect(jsonPath("pickupServicePoint.description", is("test description")))
      .andExpect(jsonPath("pickupServicePoint.shelvingLagTime", is(99)))
      .andExpect(jsonPath("pickupServicePoint.pickupLocation", is(true)))
      .andExpect(jsonPath("deliveryAddress").doesNotExist())
      .andExpect(jsonPath("searchIndex").doesNotExist())
      .andExpect(jsonPath("metadata.createdDate").exists())
      .andExpect(jsonPath("metadata.createdByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.createdByUsername").doesNotExist())
      .andExpect(jsonPath("metadata.updatedDate").exists())
      .andExpect(jsonPath("metadata.updatedByUserId", is(USER_ID)))
      .andExpect(jsonPath("metadata.updatedByUsername").doesNotExist())
      .andReturn().getResponse().getContentAsString();

    return OBJECT_MAPPER.readValue(responseBody, MediatedRequest.class);
  }

  @SneakyThrows
  private ResultActions getRequestById(String id) {
    return mockMvc.perform(
        get(URL_MEDIATED_REQUESTS + "/" + id)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  private ResultActions getRequestsByQuery(String query) {
    return mockMvc.perform(
      get(URL_MEDIATED_REQUESTS)
        .queryParam("query", query)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  private ResultActions getAllRequests() {
    return mockMvc.perform(
      get(URL_MEDIATED_REQUESTS)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  private ResultActions postRequest(MediatedRequest request) {
    return mockMvc.perform(
      post(URL_MEDIATED_REQUESTS)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)));
  }

  @SneakyThrows
  private ResultActions putRequest(MediatedRequest request) {
    return mockMvc.perform(
      put(URL_MEDIATED_REQUESTS + "/" + request.getId())
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(request)));
  }

  @SneakyThrows
  private ResultActions deleteRequest(String id) {
    return mockMvc.perform(
      delete(URL_MEDIATED_REQUESTS + "/" + id)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON));
  }

}
