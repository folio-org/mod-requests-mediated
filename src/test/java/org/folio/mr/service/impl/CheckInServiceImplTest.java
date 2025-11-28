package org.folio.mr.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.mr.client.CheckInClient;
import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.domain.dto.CheckInResponseLoan;
import org.folio.mr.domain.dto.CheckInResponseLoanBorrower;
import org.folio.mr.domain.dto.CheckInResponseLoanItem;
import org.folio.mr.domain.dto.CheckInResponseStaffSlipContext;
import org.folio.mr.domain.dto.CheckInResponseStaffSlipContextRequest;
import org.folio.mr.domain.dto.CheckInResponseStaffSlipContextRequester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckInServiceImplTest {

  private static final String ITEM_BARCODE = "item-barcode-123";
  private UUID itemId = UUID.randomUUID();

  @Mock
  private CheckInClient checkInClient;

  @InjectMocks
  private CheckInServiceImpl checkInService;

  @Test
  void checkInShouldRemovePersonalDataFromBothLoanAndStaffSlipContext() {
    CheckInRequest request = buildRequest();
    CheckInResponseLoanItem item = buildLoanItem();
    CheckInResponseLoanBorrower borrower = buildBorrower();
    CheckInResponseLoan loan = buildLoan("loan-id", "user-id", item, borrower);
    CheckInResponseStaffSlipContextRequester requester = buildRequester();
    CheckInResponseStaffSlipContext staffSlipContext = buildStaffSlipContext(requester);
    CheckInResponse response = buildResponse(loan, staffSlipContext);

    when(checkInClient.checkIn(request)).thenReturn(response);

    CheckInResponse result = checkInService.checkIn(request);

    assertThat(result, notNullValue());
    assertThat(result.getLoan(), notNullValue());
    assertThat(result.getLoan().getId(), nullValue());
    assertThat(result.getLoan().getUserId(), nullValue());
    assertThat(result.getLoan().getBorrower(), nullValue());
    assertThat(result.getLoan().getItem(), notNullValue());
    assertThat(result.getStaffSlipContext(), notNullValue());
    assertThat(result.getStaffSlipContext().getRequester(), nullValue());
    assertThat(result.getStaffSlipContext().getRequest(), nullValue());
    verify(checkInClient).checkIn(request);
  }

  @Test
  void checkInShouldHandleResponseWithNoLoan() {
    CheckInRequest request = buildRequest();
    CheckInResponse response = buildResponse(null, null);

    when(checkInClient.checkIn(request)).thenReturn(response);

    CheckInResponse result = checkInService.checkIn(request);

    assertThat(result, notNullValue());
    assertThat(result.getLoan(), nullValue());
    verify(checkInClient).checkIn(request);
  }

  @Test
  void checkInShouldHandleResponseWithNoStaffSlipContext() {
    CheckInRequest request = buildRequest();
    CheckInResponseLoanItem item = buildLoanItem();
    CheckInResponseLoanBorrower borrower = buildBorrower();
    CheckInResponseLoan loan = buildLoan("loan-id", "user-id", item, borrower);
    CheckInResponse response = buildResponse(loan, null);

    when(checkInClient.checkIn(request)).thenReturn(response);

    CheckInResponse result = checkInService.checkIn(request);

    assertThat(result, notNullValue());
    assertThat(result.getLoan(), notNullValue());
    assertThat(result.getLoan().getId(), nullValue());
    assertThat(result.getLoan().getUserId(), nullValue());
    assertThat(result.getLoan().getBorrower(), nullValue());
    assertThat(result.getStaffSlipContext(), nullValue());
    verify(checkInClient).checkIn(request);
  }

  private CheckInRequest buildRequest() {
    CheckInRequest request = new CheckInRequest();
    request.setItemBarcode(ITEM_BARCODE);
    return request;
  }

  private CheckInResponseLoanItem buildLoanItem() {
    CheckInResponseLoanItem item = new CheckInResponseLoanItem();
    item.setId(itemId.toString());
    return item;
  }

  private CheckInResponseLoanBorrower buildBorrower() {
    CheckInResponseLoanBorrower borrower = new CheckInResponseLoanBorrower();
    borrower.setFirstName("John");
    borrower.setLastName("Doe");
    borrower.setBarcode("borrower-barcode");
    return borrower;
  }

  private CheckInResponseLoan buildLoan(String id, String userId, CheckInResponseLoanItem item,
    CheckInResponseLoanBorrower borrower) {
    CheckInResponseLoan loan = new CheckInResponseLoan();
    loan.setId(id);
    loan.setUserId(userId);
    loan.setItem(item);
    loan.setBorrower(borrower);
    return loan;
  }

  private CheckInResponseStaffSlipContextRequester buildRequester() {
    CheckInResponseStaffSlipContextRequester requester = new CheckInResponseStaffSlipContextRequester();
    requester.setFirstName("Jane");
    requester.setLastName("Smith");
    requester.setBarcode("requester-barcode");
    return requester;
  }

  private CheckInResponseStaffSlipContextRequest buildStaffSlipContextRequest() {
    CheckInResponseStaffSlipContextRequest request = new CheckInResponseStaffSlipContextRequest();
    request.setRequestID("request-id-123");
    request.setServicePointPickup("Main Library");
    request.setPatronComments("Please hold for pickup");
    return request;
  }

  private CheckInResponseStaffSlipContext buildStaffSlipContext(
    CheckInResponseStaffSlipContextRequester requester) {
    CheckInResponseStaffSlipContext context = new CheckInResponseStaffSlipContext();
    context.setRequester(requester);
    context.setRequest(buildStaffSlipContextRequest());
    return context;
  }

  private CheckInResponse buildResponse(CheckInResponseLoan loan,
    CheckInResponseStaffSlipContext staffSlipContext) {
    CheckInResponse response = new CheckInResponse();
    response.setLoan(loan);
    response.setStaffSlipContext(staffSlipContext);
    return response;
  }
}
