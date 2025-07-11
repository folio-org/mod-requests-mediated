package org.folio.mr.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.asJsonString;

import org.apache.http.HttpStatus;
import org.folio.mr.domain.dto.BatchIds;
import org.folio.mr.domain.dto.CheckOutDryRunRequest;
import org.folio.mr.domain.dto.CheckOutDryRunResponse;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.ConsortiumItems;
import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.Request;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

public class MockHelper {
  private static final String ITEM_SEARCH_URL = "/search/consortium/item";
  private static final String ITEM_BATCH_SEARCH_URL = "/search/consortium/batch/items";
  private static final String CIRCULATION_STORAGE_REQUESTS_URL = "/request-storage/requests";
  private static final String CIRCULATION_CHECK_OUT_URL = "/circulation/check-out-by-barcode";
  private static final String CIRCULATION_CHECK_OUT_DRY_RUN_URL =
    "/circulation/check-out-by-barcode-dry-run";
  private static final String LOAN_POLICIES_URL = "/loan-policy-storage/loan-policies";
  private static final String LOAN_STORAGE_URL = "/loan-storage/loans/";

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

}
