package org.folio.mr.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpStatus;

import org.folio.mr.client.CirculationClient;
import org.folio.mr.client.SettingsClient;
import org.folio.mr.domain.dto.BatchIds;
import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.domain.dto.CheckOutDryRunRequest;
import org.folio.mr.domain.dto.CheckOutDryRunResponse;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.HoldingsRecord;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.ItemStatus;
import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.SearchInstancesResponse;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserTenantsResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

public class MockHelper {

  private static final String ITEM_SEARCH_URL = "/search/consortium/item";
  private static final String ITEM_BATCH_SEARCH_URL = "/search/consortium/batch/items";
  private static final String CIRCULATION_STORAGE_REQUESTS_URL = "/request-storage/requests";
  private static final String CIRCULATION_CHECK_OUT_URL = "/circulation/check-out-by-barcode";
  private static final String CIRCULATION_CHECK_IN_URL = "/circulation/check-in-by-barcode";
  private static final String CIRCULATION_CHECK_OUT_DRY_RUN_URL =
    "/circulation/check-out-by-barcode-dry-run";
  private static final String LOAN_POLICIES_URL = "/loan-policy-storage/loan-policies";
  private static final String LOAN_STORAGE_URL = "/loan-storage/loans/";
  private static final String ALLOWED_SERVICE_POINTS_URL_PATH =
    "/circulation/requests/allowed-service-points";
  private static final String USER_TENANTS_URL_PATH = "/user-tenants";
  private static final String SETTING_ENTRIES_URL_PATH = "/settings/entries";
  private static final String ITEMS_BY_ID_URL_PATH_TEMPLATE = "/item-storage/items/{id}";
  private static final String HOLDINGS_BY_ID_URL_PATH_TEMPLATE = "/holdings-storage/holdings/{id}";
  private static final String CIRCULATION_REQUESTS_URL = "/circulation/requests";
  private static final String CIRCULATION_REQUEST_BY_ID_URL_TEMPLATE = "/circulation/requests/{id}";
  private static final String USERS_BY_ID_URL_TEMPLATE = "/users/{id}";
  private static final String SEARCH_ITEM_URL_PATH_TEMPLATE = "/search/consortium/item/{id}";
  private static final String SEARCH_INSTANCES_URL_PATH = "/search/instances";
  private static final String ECS_TLR_EXTERNAL_URL_PATH = "/tlr/create-ecs-request-external";

  private final WireMockServer wireMockServer;

  public MockHelper(WireMockServer wireMockServer) {
    this.wireMockServer = wireMockServer;
  }

  public void mockItemSearch(String tenantId, String itemId, ConsortiumItem mockItem) {
    wireMockServer.stubFor(get(urlPathEqualTo(ITEM_SEARCH_URL + "/" + itemId))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(mockItem))));
  }

  public void mockItemBatchSearch(String tenantId, BatchIds batchIds, ConsortiumItems consortiumItems) {
    wireMockServer.stubFor(post(urlPathEqualTo(ITEM_BATCH_SEARCH_URL))
      .withRequestBody(equalTo(asJsonString(batchIds)))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(consortiumItems))));
  }

  public void mockGetRequest(Request request, String tenantId) {
    wireMockServer.stubFor(get(urlPathEqualTo(CIRCULATION_STORAGE_REQUESTS_URL + "/" + request.getId()))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(request))));
  }

  public void mockGetLoan(Loan loan, String tenantId) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(LOAN_STORAGE_URL + loan.getId()))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(loan))));
  }

  public void mockCirculationCheckOut(CheckOutRequest request, CheckOutResponse response,
    String tenantId) {

    wireMockServer.stubFor(post(urlEqualTo(CIRCULATION_CHECK_OUT_URL))
      .withRequestBody(equalTo(asJsonString(request)))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(response))));
  }

  public void mockCirculationCheckOutDryRun(CheckOutDryRunRequest request,
    CheckOutDryRunResponse response, String tenantId) {

    wireMockServer.stubFor(post(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(response))));
  }

  public void mockGetLoanPolicy(LoanPolicy loanPolicy, String tenantId) {
    wireMockServer.stubFor(get(urlEqualTo(LOAN_POLICIES_URL + "/" + loanPolicy.getId()))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(loanPolicy))));
  }

  public void mockPostLoanPolicy(LoanPolicy loanPolicy, String tenantId) {
    wireMockServer.stubFor(post(urlEqualTo(LOAN_POLICIES_URL))
      .withRequestBody(equalToJson(asJsonString(loanPolicy)))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(asJsonString(loanPolicy), HttpStatus.SC_CREATED)));
  }

  public void mockPostNoContentResponse(String url, String tenant) {
    wireMockServer.stubFor(post(urlPathEqualTo(url))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(noContent()));
  }

  public void mockGetNotFoundResponse(String url, String tenant) {
    wireMockServer.stubFor(get(urlPathEqualTo(url))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(notFound()));
  }

  public void mockCirculationCheckIn(CheckInRequest request, CheckInResponse response,
    String tenantId) {

    wireMockServer.stubFor(post(urlEqualTo(CIRCULATION_CHECK_IN_URL))
      .withRequestBody(matchingJsonPath("$.itemBarcode", equalTo(request.getItemBarcode())))
      .withRequestBody(matchingJsonPath("$.servicePointId", equalTo(request.getServicePointId().toString())))
      .withHeader(TENANT, equalTo(tenantId))
      .willReturn(okJson(asJsonString(response))));
  }

  public void mockPostCirculationRequestAny(String tenant) {
    wireMockServer.stubFor(post(urlEqualTo(CIRCULATION_REQUESTS_URL))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(aResponse()
        .withStatus(HttpStatus.SC_CREATED)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withTransformers("response-template")
        .withBody("""
          {
            "id": "{{randomValue type='UUID'}}",
            "requestLevel": "Item",
            "requestType": "Hold",
            "status": "Open - Not yet filled",
            "itemId": "{{jsonPath request.body '$.itemId'}}",
            "instanceId": "{{jsonPath request.body '$.instanceId'}}",
            "requesterId": "{{jsonPath request.body '$.requesterId'}}",
            "holdingsRecordId": "{{jsonPath request.body '$.holdingsRecordId'}}",
            "pickupServicePointId": "{{jsonPath request.body '$.pickupServicePointId'}}"
          }
          """
        )));
  }

  public void mockGetUserTenants(String tenant, UserTenantsResponse response) {
    wireMockServer.stubFor(get(urlPathEqualTo(USER_TENANTS_URL_PATH))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(jsonResponse(asJsonString(response), HttpStatus.SC_OK)));
  }

  public void mockGetSettingEntries(String tenant, String query,
    SettingsClient.SettingsEntries response) {

    wireMockServer.stubFor(get(urlPathEqualTo(SETTING_ENTRIES_URL_PATH))
      .withQueryParam("query", containing(query))
      .withQueryParam("limit", equalTo("1"))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(jsonResponse(asJsonString(response), HttpStatus.SC_OK)));
  }

  public void mockGetAllowedServicePoints(String tenant, CirculationClient.AllowedServicePoints response) {
    wireMockServer.stubFor(get(urlPathEqualTo(ALLOWED_SERVICE_POINTS_URL_PATH))
      .withQueryParam("requesterId", matching(".{36}"))
      .withQueryParam("operation", equalTo("create"))
      .withQueryParam("itemId", matching(".{36}"))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(jsonResponse(asJsonString(response), HttpStatus.SC_OK))
    );
  }

  public void mockGetInventoryHoldingRecord(String tenant, String holdingId) {
    var holding = new HoldingsRecord().id(holdingId).instanceId(UUID.randomUUID().toString());
    wireMockServer.stubFor(get(urlPathTemplate(HOLDINGS_BY_ID_URL_PATH_TEMPLATE))
      .withPathParam("id", equalTo(holdingId))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(jsonResponse(asJsonString(holding), HttpStatus.SC_OK)));
  }

  public void mockGetInventoryItemAny(String tenant, String holdingRecordId) {
    var item = new Item()
      .id("{{request.path.id}}")
      .barcode("batchRequestTest")
      .holdingsRecordId(holdingRecordId)
      .status(new ItemStatus().name(ItemStatus.NameEnum.AVAILABLE));

    wireMockServer.stubFor(get(urlPathTemplate(ITEMS_BY_ID_URL_PATH_TEMPLATE))
      .withHeader(TENANT, equalTo(tenant))
      .withPathParam("id", matching(".{36}"))
      .willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withTransformers("response-template")
        .withBody(asJsonString(item))));
  }

  public void mockPostEcsExternalRequestAny(String tenant) {
    var ecsTlr = new EcsTlr()
      .id(UUID.randomUUID().toString())
      .requesterId("{{jsonPath request.body '$.holdingsRecordId'}}")
      .instanceId("{{jsonPath request.body '$.instanceId'}}")
      .itemId("{{jsonPath request.body '$.itemId'}}")
      .primaryRequestId(UUID.randomUUID().toString());
    wireMockServer.stubFor(post(urlEqualTo(ECS_TLR_EXTERNAL_URL_PATH))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(aResponse()
        .withStatus(HttpStatus.SC_CREATED)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withTransformers("response-template")
        .withBody(asJsonString(ecsTlr))));
  }

  public void mockGetCirculationRequestByIdAny(String tenant, Request request) {
    wireMockServer.stubFor(get(urlPathTemplate(CIRCULATION_REQUEST_BY_ID_URL_TEMPLATE))
      .withPathParam("id", matching(".{36}"))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withTransformers("response-template")
        .withBody(asJsonString(request))));
  }

  public void mockGetConsortiumItemAny(String tenant, ConsortiumItem item) {
    wireMockServer.stubFor(get(urlPathTemplate(SEARCH_ITEM_URL_PATH_TEMPLATE))
      .withHeader(TENANT, equalTo(tenant))
      .withPathParam("id", matching(".{36}"))
      .willReturn(aResponse()
        .withTransformers("response-template")
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withBody(asJsonString(item))));
  }

  public void mockGetSearchInstancesEmpty(String tenant, SearchInstancesResponse instances) {
    wireMockServer.stubFor(get(urlPathEqualTo(SEARCH_INSTANCES_URL_PATH))
      .withHeader(TENANT, equalTo(tenant))
      .willReturn(aResponse()
        .withStatus(HttpStatus.SC_OK)
        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
        .withTransformers("response-template")
        .withBody(asJsonString(instances))));
  }

  public void mockGetUserById(String tenant, User user) {
    wireMockServer.stubFor(WireMock.get(urlPathTemplate(USERS_BY_ID_URL_TEMPLATE))
      .withHeader(TENANT, equalTo(tenant))
      .withPathParam("id", equalTo(user.getId()))
      .willReturn(jsonResponse(asJsonString(user), HttpStatus.SC_OK)));
  }
}
