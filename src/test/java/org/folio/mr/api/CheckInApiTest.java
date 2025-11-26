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
import org.folio.mr.domain.dto.CheckInResponseLoanItem;
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

  private static final String ITEM_BARCODE = "item_barcode";

  @Test
  @SneakyThrows
  void checkInWithLoanInResponse() {
    CheckInResponseLoan loan = buildCheckInResponseLoan();
    CheckInResponse circulationCheckInResponse = buildCheckInResponse(loan);

    mockCirculationCheckIn(circulationCheckInResponse);

    checkIn(buildCheckInRequest(ITEM_BARCODE))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loan.id").doesNotExist())
      .andExpect(jsonPath("$.loan.userId").doesNotExist())
      .andExpect(jsonPath("$.loan.borrower").doesNotExist())
      .andExpect(jsonPath("$.loan.item.id").value(ITEM_ID.toString()));

    verifyCirculationCheckInCalled();
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
  }

  private void mockCirculationCheckIn(CheckInResponse response) {
    mockHelper.mockCirculationCheckIn(buildCheckInRequest(ITEM_BARCODE), response, TENANT_ID_CONSORTIUM);
  }

  private void verifyCirculationCheckInCalled() {
    wireMockServer.verify(1, postRequestedFor(urlEqualTo(CIRCULATION_CHECK_IN_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
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


  @SneakyThrows
  private ResultActions checkIn(CheckInRequest request) {
    return mockMvc.perform(MockMvcRequestBuilders.post(MEDIATED_REQUESTS_CHECK_IN_URL)
      .headers(defaultHeaders())
      .contentType(MediaType.APPLICATION_JSON)
      .content(asJsonString(request)));
  }

}

