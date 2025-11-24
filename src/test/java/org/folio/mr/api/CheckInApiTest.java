package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.domain.dto.CheckInResponseLoan;
import org.folio.mr.domain.dto.CheckInResponseLoanItem;
import org.folio.mr.domain.dto.Loan;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import lombok.SneakyThrows;

@IntegrationTest
class CheckInApiTest extends BaseIT {

  private static final UUID SERVICE_POINT_ID = UUID.randomUUID();
  private static final UUID ITEM_ID = UUID.randomUUID();
  private static final UUID LOAN_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final Date CHECK_IN_DATE = new Date();

  private static final String MEDIATED_REQUESTS_CHECK_IN_URL =
    "/requests-mediated/loans/check-in-by-barcode";
  private static final String CIRCULATION_CHECK_IN_URL = "/circulation/check-in-by-barcode";
  private static final String CIRCULATION_STORAGE_LOANS_URL = "/loan-storage/loans";

  private static final String ITEM_BARCODE = "item_barcode";

  @Test
  @SneakyThrows
  void checkInWithLoanInResponse() {
    CheckInResponseLoan loan = buildCheckInResponseLoan();
    CheckInResponse circulationCheckInResponse = buildCheckInResponse(loan);
    Loan centralLoan = buildCentralLoan();

    mockCirculationCheckIn(circulationCheckInResponse);
    mockHelper.mockGetUserTenants();
    mockHelper.mockGetOpenLoanByItemId(ITEM_ID.toString(), centralLoan, TENANT_ID_CENTRAL);

    checkIn(buildCheckInRequest(ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loan.id").value(LOAN_ID.toString()))
      .andExpect(jsonPath("$.loan.userId").value(USER_ID.toString()))
      .andExpect(jsonPath("$.loan.item.id").value(ITEM_ID.toString()));

    verifyCirculationCheckInCalled();
    verifyLoanStorageCalledForCentralTenant();
  }

  @Test
  @SneakyThrows
  void checkInWithoutLoanInResponse() {
    CheckInResponse circulationCheckInResponse = buildCheckInResponse(null);

    mockCirculationCheckIn(circulationCheckInResponse);

    checkIn(buildCheckInRequest(ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(circulationCheckInResponse)));

    verifyCirculationCheckInCalled();
    verifyLoanStorageNotCalled();
  }

  @Test
  @SneakyThrows
  void checkInWhenCentralTenantIsNotFound() {
    CheckInResponseLoan loan = buildCheckInResponseLoan();
    CheckInResponse circulationCheckInResponse = buildCheckInResponse(loan);

    mockCirculationCheckIn(circulationCheckInResponse);
    mockHelper.mockGetUserTenantsEmpty();

    checkIn(buildCheckInRequest(ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(circulationCheckInResponse)));

    verifyCirculationCheckInCalled();
    verifyLoanStorageNotCalled();
  }

  @Test
  @SneakyThrows
  void checkInWhenCentralLoanIsNotFound() {
    CheckInResponseLoan loan = buildCheckInResponseLoan();
    CheckInResponse circulationCheckInResponse = buildCheckInResponse(loan);

    mockCirculationCheckIn(circulationCheckInResponse);
    mockHelper.mockGetUserTenants();
    mockHelper.mockGetOpenLoanByItemIdNotFound(ITEM_ID.toString(), TENANT_ID_CENTRAL);

    checkIn(buildCheckInRequest(ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(circulationCheckInResponse)));

    verifyCirculationCheckInCalled();
    verifyLoanStorageCalledForCentralTenant();
  }

  @ParameterizedTest
  @ValueSource(ints = { 400, 422, 500 })
  @SneakyThrows
  void circulationCheckInErrorsAreForwarded(int checkInResponseStatus) {
    wireMockServer.stubFor(post(urlEqualTo(CIRCULATION_CHECK_IN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(aResponse().withStatus(checkInResponseStatus)
        .withBody("Response status is " + checkInResponseStatus)));

    checkIn(buildCheckInRequest(ITEM_BARCODE))
      .andExpect(status().is(checkInResponseStatus))
      .andExpect(content().string("Response status is " + checkInResponseStatus));

    verifyCirculationCheckInCalled();
    verifyLoanStorageNotCalled();
  }

  private void mockCirculationCheckIn(CheckInResponse response) {
    mockHelper.mockCirculationCheckIn(buildCheckInRequest(ITEM_BARCODE), response, TENANT_ID_CONSORTIUM);
  }

  private void verifyCirculationCheckInCalled() {
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_IN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  private void verifyLoanStorageCalledForCentralTenant() {
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(CIRCULATION_STORAGE_LOANS_URL))
      .withQueryParam("query", matching("itemId==\"?" + ITEM_ID + "\"?.*status\\.name==\"?Open\"?.*"))
      .withHeader(TENANT, equalTo(TENANT_ID_CENTRAL)));
  }

  private void verifyLoanStorageNotCalled() {
    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(CIRCULATION_STORAGE_LOANS_URL)));
  }

  private static CheckInRequest buildCheckInRequest(String itemBarcode) {
    return new CheckInRequest()
      .itemBarcode(itemBarcode)
      .servicePointId(SERVICE_POINT_ID)
      .checkInDate(CHECK_IN_DATE);
  }

  private static CheckInResponse buildCheckInResponse(CheckInResponseLoan loan) {
    return new CheckInResponse()
      .loan(loan);
  }

  private static CheckInResponseLoan buildCheckInResponseLoan() {
    return new CheckInResponseLoan()
      .id(LOAN_ID.toString())
      .userId(USER_ID.toString())
      .item(new CheckInResponseLoanItem().id(ITEM_ID.toString()));
  }

  private static Loan buildCentralLoan() {
    return new Loan()
      .id(LOAN_ID)
      .itemId(ITEM_ID)
      .userId(USER_ID.toString());
  }

  @SneakyThrows
  private ResultActions checkIn(CheckInRequest request) {
    return mockMvc.perform(MockMvcRequestBuilders.post(MEDIATED_REQUESTS_CHECK_IN_URL)
      .headers(defaultHeaders())
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(request)));
  }

}

