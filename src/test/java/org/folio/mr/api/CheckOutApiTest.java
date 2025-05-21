package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequestEntity;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.folio.mr.domain.dto.BatchIds;
import org.folio.mr.domain.dto.CheckOutDryRunRequest;
import org.folio.mr.domain.dto.CheckOutDryRunResponse;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.RequestRequester;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import lombok.SneakyThrows;

@IntegrationTest
class CheckOutApiTest extends BaseIT {

  private static final UUID SERVICE_POINT_ID = UUID.randomUUID();
  private static final UUID CONFIRMED_REQUEST_ID = UUID.randomUUID();
  private static final UUID LOAN_POLICY_ID = UUID.randomUUID();

  private static final String MEDIATED_REQUESTS_CHECK_OUT_URL =
    "/requests-mediated/loans/check-out-by-barcode";
  private static final String ITEM_BATCH_SEARCH_URL = "/search/consortium/batch/items";
  private static final String CIRCULATION_STORAGE_REQUESTS_URL = "/request-storage/requests";
  private static final String CIRCULATION_CHECK_OUT_URL = "/circulation/check-out-by-barcode";
  private static final String CIRCULATION_CHECK_OUT_DRY_RUN_URL =
    "/circulation/check-out-by-barcode-dry-run";
  private static final String LOAN_POLICIES_URL = "/loan-policy-storage/loan-policies";

  private static final String ITEM_BARCODE = "item_barcode";
  private static final String REAL_USER_BARCODE = "real_user_barcode";
  private static final String FAKE_USER_BARCODE = "fake_user_barcode";
  private static final String CLONED_LOAN_POLICY_NAME_PREFIX = "COPY_OF_";

  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;

  @BeforeEach
  void clearDatabase() {
    mediatedRequestsRepository.deleteAll();
  }


  @Test
  @SneakyThrows
  void ecsCheckOutWithLoanPolicyCloning() {
    // mock mediated request
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(OPEN_AWAITING_PICKUP)
      .withRequesterBarcode(REAL_USER_BARCODE)
      .withItemBarcode(ITEM_BARCODE)
      .withConfirmedRequestId(CONFIRMED_REQUEST_ID);

    mediatedRequestsRepository.save(mediatedRequest);

    // mock item search
    mockHelper.mockItemBatchSearch(TENANT_ID_CENTRAL, buildBatchIds(),
      buildItemBatchSearchResponse(ITEM_BARCODE, TENANT_ID_COLLEGE));

    // mock secondary request
    mockHelper.mockGetRequest(buildRequest(mediatedRequest, FAKE_USER_BARCODE), TENANT_ID_COLLEGE);

    // mock check-out dry run
    mockHelper.mockCirculationCheckOutDryRun(
      buildCheckOutDryRunRequest(FAKE_USER_BARCODE, ITEM_BARCODE),
      buildCheckOutDryRunResponse(LOAN_POLICY_ID.toString()),
      TENANT_ID_COLLEGE);

    // mock loan policy
    wireMockServer.stubFor(get(urlEqualTo(LOAN_POLICIES_URL + "/" + LOAN_POLICY_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)) // local tenant
      .willReturn(notFound()));

    LoanPolicy mockOriginalLoanPolicy = buildLoanPolicy("Test loan policy");
    mockHelper.mockGetLoanPolicy(mockOriginalLoanPolicy, TENANT_ID_COLLEGE);

    LoanPolicy mockClonedLoanPolicy = buildLoanPolicy(
      CLONED_LOAN_POLICY_NAME_PREFIX + mockOriginalLoanPolicy.getName());

    mockHelper.mockPostLoanPolicy(mockClonedLoanPolicy, TENANT_ID_CONSORTIUM);

    // mock circulation check-out
    CheckOutRequest circulationCheckOutRequest = buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE)
      .forceLoanPolicyId(LOAN_POLICY_ID);

    CheckOutResponse circulationCheckOutResponse = buildCheckOutResponse();
    mockHelper.mockCirculationCheckOut(circulationCheckOutRequest, circulationCheckOutResponse,
      TENANT_ID_CONSORTIUM);

    // check out
    checkOut(buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(circulationCheckOutResponse)));

    wireMockServer.verify(1, postRequestedFor(urlEqualTo(ITEM_BATCH_SEARCH_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(CIRCULATION_STORAGE_REQUESTS_URL + "/" +
      mediatedRequest.getConfirmedRequestId().toString()))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(LOAN_POLICIES_URL + "/" + LOAN_POLICY_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(LOAN_POLICIES_URL + "/" + LOAN_POLICY_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(LOAN_POLICIES_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void ecsCheckOutWithExistingClonedLoanPolicy() {
    // mock mediated request
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(OPEN_AWAITING_PICKUP)
      .withRequesterBarcode(REAL_USER_BARCODE)
      .withItemBarcode(ITEM_BARCODE)
      .withConfirmedRequestId(CONFIRMED_REQUEST_ID);

    mediatedRequestsRepository.save(mediatedRequest);

    // mock item search
    mockHelper.mockItemBatchSearch(TENANT_ID_CENTRAL, buildBatchIds(),
      buildItemBatchSearchResponse(ITEM_BARCODE, TENANT_ID_COLLEGE));

    // mock secondary request
    mockHelper.mockGetRequest(buildRequest(mediatedRequest, FAKE_USER_BARCODE), TENANT_ID_COLLEGE);

    // mock check-out dry run
    mockHelper.mockCirculationCheckOutDryRun(
      buildCheckOutDryRunRequest(FAKE_USER_BARCODE, ITEM_BARCODE),
      buildCheckOutDryRunResponse(LOAN_POLICY_ID.toString()),
      TENANT_ID_COLLEGE);

    // mock loan policy
    LoanPolicy existingClonedLoanPolicy = buildLoanPolicy("COPY_OF_Test loan policy");
    mockHelper.mockGetLoanPolicy(existingClonedLoanPolicy, TENANT_ID_CONSORTIUM);

    // mock circulation check-out
    CheckOutRequest circulationCheckOutRequest = buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE)
      .forceLoanPolicyId(LOAN_POLICY_ID);

    CheckOutResponse circulationCheckOutResponse = buildCheckOutResponse();
    mockHelper.mockCirculationCheckOut(circulationCheckOutRequest, circulationCheckOutResponse,
      TENANT_ID_CONSORTIUM);

    // check out
    checkOut(buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(circulationCheckOutResponse)));

    wireMockServer.verify(1, postRequestedFor(urlEqualTo(ITEM_BATCH_SEARCH_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(CIRCULATION_STORAGE_REQUESTS_URL + "/" +
      mediatedRequest.getConfirmedRequestId().toString()))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(LOAN_POLICIES_URL + "/" + LOAN_POLICY_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(0, getRequestedFor(urlEqualTo(LOAN_POLICIES_URL + "/" + LOAN_POLICY_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(LOAN_POLICIES_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void regularCheckOutWhenMediatedRequestIsNotFound() {
    mockHelper.mockItemBatchSearch(TENANT_ID_CENTRAL, buildBatchIds(),
      buildItemBatchSearchResponse(ITEM_BARCODE, TENANT_ID_COLLEGE));

    CheckOutRequest circulationCheckOutRequest = buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE);
    CheckOutResponse circulationCheckOutResponse = buildCheckOutResponse();
    mockHelper.mockCirculationCheckOut(circulationCheckOutRequest, circulationCheckOutResponse,
      TENANT_ID_CONSORTIUM);

    checkOut(buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(circulationCheckOutResponse)));

    wireMockServer.verify(0, postRequestedFor(urlEqualTo(ITEM_BATCH_SEARCH_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(0, getRequestedFor(urlMatching(CIRCULATION_STORAGE_REQUESTS_URL + ".*")));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL)));
    wireMockServer.verify(0, getRequestedFor(urlMatching(LOAN_POLICIES_URL + ".*")));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(LOAN_POLICIES_URL)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void regularCheckOutWhenItemIsInLocalTenant() {
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(OPEN_AWAITING_PICKUP)
      .withRequesterBarcode(REAL_USER_BARCODE)
      .withItemBarcode(ITEM_BARCODE)
      .withConfirmedRequestId(CONFIRMED_REQUEST_ID);

    mediatedRequestsRepository.save(mediatedRequest);

    mockHelper.mockItemBatchSearch(TENANT_ID_CENTRAL, buildBatchIds(),
      buildItemBatchSearchResponse(ITEM_BARCODE, TENANT_ID_CONSORTIUM));

    CheckOutRequest circulationCheckOutRequest = buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE);
    CheckOutResponse circulationCheckOutResponse = buildCheckOutResponse();
    mockHelper.mockCirculationCheckOut(circulationCheckOutRequest, circulationCheckOutResponse,
      TENANT_ID_CONSORTIUM);

    checkOut(buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(circulationCheckOutResponse)));

    wireMockServer.verify(1, postRequestedFor(urlEqualTo(ITEM_BATCH_SEARCH_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(0, getRequestedFor(urlMatching(CIRCULATION_STORAGE_REQUESTS_URL + ".*")));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL)));
    wireMockServer.verify(0, getRequestedFor(urlMatching(LOAN_POLICIES_URL + ".*")));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(LOAN_POLICIES_URL)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void checkOutFailsWithWhenItemIsNotFound() {
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(OPEN_AWAITING_PICKUP)
      .withRequesterBarcode(REAL_USER_BARCODE)
      .withItemBarcode(ITEM_BARCODE)
      .withConfirmedRequestId(CONFIRMED_REQUEST_ID);

    mediatedRequestsRepository.save(mediatedRequest);

    wireMockServer.stubFor(post(urlPathEqualTo(ITEM_BATCH_SEARCH_URL))
      .withRequestBody(equalTo(asJsonString(buildBatchIds())))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL))
      .willReturn(notFound()));

    checkOut(buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE))
      .andExpect(status().isNotFound());

    wireMockServer.verify(1, postRequestedFor(urlEqualTo(ITEM_BATCH_SEARCH_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(0, getRequestedFor(urlMatching(CIRCULATION_STORAGE_REQUESTS_URL + ".*")));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL)));
    wireMockServer.verify(0, getRequestedFor(urlMatching(LOAN_POLICIES_URL + ".*")));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(LOAN_POLICIES_URL)));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_URL)));
  }

  @Test
  @SneakyThrows
  void checkOutFailsWhenSecondaryRequestIsNotFound() {
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(OPEN_AWAITING_PICKUP)
      .withRequesterBarcode(REAL_USER_BARCODE)
      .withItemBarcode(ITEM_BARCODE)
      .withConfirmedRequestId(CONFIRMED_REQUEST_ID);

    mediatedRequestsRepository.save(mediatedRequest);

    mockHelper.mockItemBatchSearch(TENANT_ID_CENTRAL, buildBatchIds(),
      buildItemBatchSearchResponse(ITEM_BARCODE, TENANT_ID_COLLEGE));

    wireMockServer.stubFor(get(urlPathEqualTo(CIRCULATION_STORAGE_REQUESTS_URL + "/" + CONFIRMED_REQUEST_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(notFound()));

    checkOut(buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE))
      .andExpect(status().isNotFound());

    wireMockServer.verify(1, postRequestedFor(urlEqualTo(ITEM_BATCH_SEARCH_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(
      CIRCULATION_STORAGE_REQUESTS_URL + "/" + CONFIRMED_REQUEST_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL)));
    wireMockServer.verify(0, getRequestedFor(urlMatching(LOAN_POLICIES_URL + ".*")));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(LOAN_POLICIES_URL)));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_URL)));
  }

  @Test
  @SneakyThrows
  void checkOutFailsWhenDryRunFails() {
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(OPEN_AWAITING_PICKUP)
      .withRequesterBarcode(REAL_USER_BARCODE)
      .withItemBarcode(ITEM_BARCODE)
      .withConfirmedRequestId(CONFIRMED_REQUEST_ID);

    mediatedRequestsRepository.save(mediatedRequest);

    mockHelper.mockItemBatchSearch(TENANT_ID_CENTRAL, buildBatchIds(),
      buildItemBatchSearchResponse(ITEM_BARCODE, TENANT_ID_COLLEGE));

    mockHelper.mockGetRequest(buildRequest(mediatedRequest, FAKE_USER_BARCODE), TENANT_ID_COLLEGE);

    wireMockServer.stubFor(post(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL))
      .withRequestBody(equalToJson(asJsonString(buildCheckOutDryRunRequest(FAKE_USER_BARCODE, ITEM_BARCODE))))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(serverError()));

    checkOut(buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE))
      .andExpect(status().is5xxServerError());

    wireMockServer.verify(1, postRequestedFor(urlEqualTo(ITEM_BATCH_SEARCH_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(
      CIRCULATION_STORAGE_REQUESTS_URL + "/" +  CONFIRMED_REQUEST_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(0, getRequestedFor(urlMatching(LOAN_POLICIES_URL + ".*")));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(LOAN_POLICIES_URL)));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_URL)));
  }

  @ParameterizedTest
  @ValueSource(ints = { 400, 422, 500 })
  @SneakyThrows
  void circulationCheckOutErrorsAreForwarded(int checkOutResponseStatus) {
    MediatedRequestEntity mediatedRequest = buildMediatedRequestEntity(OPEN_AWAITING_PICKUP)
      .withRequesterBarcode(REAL_USER_BARCODE)
      .withItemBarcode(ITEM_BARCODE)
      .withConfirmedRequestId(CONFIRMED_REQUEST_ID);

    mediatedRequestsRepository.save(mediatedRequest);

    mockHelper.mockItemBatchSearch(TENANT_ID_CENTRAL, buildBatchIds(),
      buildItemBatchSearchResponse(ITEM_BARCODE, TENANT_ID_COLLEGE));

    mockHelper.mockGetRequest(buildRequest(mediatedRequest, FAKE_USER_BARCODE), TENANT_ID_COLLEGE);

    mockHelper.mockCirculationCheckOutDryRun(
      buildCheckOutDryRunRequest(FAKE_USER_BARCODE, ITEM_BARCODE),
      buildCheckOutDryRunResponse(LOAN_POLICY_ID.toString()),
      TENANT_ID_COLLEGE);

    LoanPolicy existingClonedLoanPolicy = buildLoanPolicy("COPY_OF_Test loan policy");
    mockHelper.mockGetLoanPolicy(existingClonedLoanPolicy, TENANT_ID_CONSORTIUM);

    CheckOutRequest circulationCheckOutRequest = buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE)
      .forceLoanPolicyId(LOAN_POLICY_ID);

    wireMockServer.stubFor(post(urlEqualTo(CIRCULATION_CHECK_OUT_URL))
      .withRequestBody(equalTo(asJsonString(circulationCheckOutRequest)))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(aResponse().withStatus(checkOutResponseStatus)
        .withBody("Response status is " + checkOutResponseStatus)));

    checkOut(buildCheckOutRequest(REAL_USER_BARCODE, ITEM_BARCODE))
      .andExpect(status().is(checkOutResponseStatus))
      .andExpect(content().string("Response status is " + checkOutResponseStatus));

    wireMockServer.verify(1, postRequestedFor(urlEqualTo(ITEM_BATCH_SEARCH_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(CIRCULATION_STORAGE_REQUESTS_URL + "/" +
      mediatedRequest.getConfirmedRequestId().toString()))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_DRY_RUN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, getRequestedFor(urlEqualTo(LOAN_POLICIES_URL + "/" + LOAN_POLICY_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(0, getRequestedFor(urlEqualTo(LOAN_POLICIES_URL + "/" + LOAN_POLICY_ID))
      .withHeader(TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(0, postRequestedFor(urlEqualTo(LOAN_POLICIES_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_OUT_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  private static LoanPolicy buildLoanPolicy(String name) {
    return new LoanPolicy()
      .id(LOAN_POLICY_ID.toString())
      .name(name);
  }

  private static Request buildRequest(MediatedRequestEntity mediatedRequest, String requesterBarcode) {
    return new Request()
      .id(mediatedRequest.getConfirmedRequestId().toString())
      .itemId(mediatedRequest.getItemId().toString())
      .requesterId(mediatedRequest.getRequesterId().toString())
      .requester(new RequestRequester().barcode(requesterBarcode));
  }

  private static BatchIds buildBatchIds() {
    return new BatchIds()
      .identifierType(BatchIds.IdentifierTypeEnum.BARCODE)
      .addIdentifierValuesItem(ITEM_BARCODE);
  }

  private static CheckOutRequest buildCheckOutRequest(String userBarcode, String itemBarcode) {
    return new CheckOutRequest()
      .userBarcode(userBarcode)
      .itemBarcode(itemBarcode)
      .servicePointId(SERVICE_POINT_ID);
  }

  private static CheckOutResponse buildCheckOutResponse() {
    return new CheckOutResponse()
      .id(randomId());
  }

  private static CheckOutDryRunRequest buildCheckOutDryRunRequest(String userBarcode, String itemBarcode) {
    return new CheckOutDryRunRequest()
      .userBarcode(userBarcode)
      .itemBarcode(itemBarcode);
  }

  private static CheckOutDryRunResponse buildCheckOutDryRunResponse(String loanPolicyId) {
    return new CheckOutDryRunResponse().loanPolicyId(loanPolicyId);
  }

  private ConsortiumItems buildItemBatchSearchResponse(String itemBarcode, String tenantId) {
    return new ConsortiumItems().addItemsItem(
      new ConsortiumItem()
        .barcode(itemBarcode)
        .tenantId(tenantId));
  }

  @SneakyThrows
  private ResultActions checkOut(CheckOutRequest request) {
    return mockMvc.perform(MockMvcRequestBuilders.post(MEDIATED_REQUESTS_CHECK_OUT_URL)
      .headers(defaultHeaders())
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(request)));
  }

}
