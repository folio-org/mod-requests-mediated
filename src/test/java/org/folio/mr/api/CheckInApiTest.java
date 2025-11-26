package org.folio.mr.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;

import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.domain.dto.CheckInResponseLoan;
import org.folio.mr.domain.dto.CheckInResponseLoanBorrower;
import org.folio.mr.domain.dto.CheckInResponseLoanItem;
import org.folio.mr.domain.dto.CheckInResponseStaffSlipContext;
import org.folio.mr.domain.dto.CheckInResponseStaffSlipContextRequester;
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

  private static final String MEDIATED_REQUESTS_CHECK_IN_URL = "/requests-mediated/loans/check-in-by-barcode";
  private static final String CIRCULATION_CHECK_IN_URL = "/circulation/check-in-by-barcode";
  private static final String ITEM_BARCODE = "item_barcode";

  private static final UUID SERVICE_POINT_ID = UUID.randomUUID();
  private static final UUID ITEM_ID = UUID.randomUUID();
  private static final UUID LOAN_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final Date CHECK_IN_DATE = new Date();

  @Test
  @SneakyThrows
  void checkInShouldRemovePersonalDataFromLoanAndStaffSlipContext() {
    CheckInResponse response = buildFullCheckInResponse();
    mockCirculationCheckIn(response);

    performCheckIn()
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loan.id").doesNotExist())
      .andExpect(jsonPath("$.loan.userId").doesNotExist())
      .andExpect(jsonPath("$.loan.borrower").doesNotExist())
      .andExpect(jsonPath("$.loan.item.id").value(ITEM_ID.toString()))
      .andExpect(jsonPath("$.staffSlipContext.requester").doesNotExist());

    verifyCirculationCheckInCalled();
  }

  @Test
  @SneakyThrows
  void checkInShouldHandleResponseWithoutLoan() {
    CheckInResponse response = buildCheckInResponse(null, null);
    mockCirculationCheckIn(response);

    performCheckIn()
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(response)));

    verifyCirculationCheckInCalled();
  }

  @ParameterizedTest
  @ValueSource(ints = { 400, 422, 500 })
  @SneakyThrows
  void checkInShouldForwardCirculationErrors(int errorStatus) {
    String errorMessage = "Response status is " + errorStatus;
    stubCirculationCheckInError(errorStatus, errorMessage);

    performCheckIn()
      .andExpect(status().is(errorStatus))
      .andExpect(content().string(errorMessage));

    verifyCirculationCheckInCalled();
  }

  private void stubCirculationCheckInError(int status, String body) {
    wireMockServer.stubFor(post(urlEqualTo(CIRCULATION_CHECK_IN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(aResponse().withStatus(status).withBody(body)));
  }

  private void mockCirculationCheckIn(CheckInResponse response) {
    mockHelper.mockCirculationCheckIn(buildCheckInRequest(), response, TENANT_ID_CONSORTIUM);
  }

  private void verifyCirculationCheckInCalled() {
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_IN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @SneakyThrows
  private ResultActions performCheckIn() {
    return checkIn(buildCheckInRequest());
  }

  @SneakyThrows
  private ResultActions checkIn(CheckInRequest request) {
    return mockMvc.perform(MockMvcRequestBuilders.post(MEDIATED_REQUESTS_CHECK_IN_URL)
      .headers(defaultHeaders())
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(request)));
  }

  private static CheckInRequest buildCheckInRequest() {
    return new CheckInRequest()
      .itemBarcode(ITEM_BARCODE)
      .servicePointId(SERVICE_POINT_ID)
      .checkInDate(CHECK_IN_DATE);
  }

  private static CheckInResponse buildFullCheckInResponse() {
    return buildCheckInResponse(buildLoanWithPersonalData(), buildStaffSlipContextWithPersonalData());
  }

  private static CheckInResponse buildCheckInResponse(CheckInResponseLoan loan,
    CheckInResponseStaffSlipContext staffSlipContext) {
    return new CheckInResponse()
      .loan(loan)
      .staffSlipContext(staffSlipContext);
  }

  private static CheckInResponseLoan buildLoanWithPersonalData() {
    return new CheckInResponseLoan()
      .id(LOAN_ID.toString())
      .userId(USER_ID.toString())
      .borrower(buildBorrower())
      .item(buildLoanItem());
  }

  private static CheckInResponseLoanBorrower buildBorrower() {
    return new CheckInResponseLoanBorrower()
      .firstName("John")
      .lastName("Doe")
      .barcode("borrower-barcode");
  }

  private static CheckInResponseLoanItem buildLoanItem() {
    return new CheckInResponseLoanItem()
      .id(ITEM_ID.toString());
  }

  private static CheckInResponseStaffSlipContext buildStaffSlipContextWithPersonalData() {
    return new CheckInResponseStaffSlipContext()
      .requester(buildRequester());
  }

  private static CheckInResponseStaffSlipContextRequester buildRequester() {
    return new CheckInResponseStaffSlipContextRequester()
      .firstName("Jane")
      .lastName("Smith")
      .barcode("requester-barcode");
  }
}

