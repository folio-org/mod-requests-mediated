package org.folio.mr.controller;

import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.domain.dto.CheckInResponseLoan;
import org.folio.mr.service.CheckInService;
import org.folio.mr.service.CheckOutService;
import org.folio.mr.service.MediatedRequestsLoansActionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediatedRequestLoansControllerTest {

    @Mock
    private CheckInService checkInService;
    @Mock
    private CheckOutService checkOutService;
    @Mock
    private MediatedRequestsLoansActionService mediatedRequestsLoansActionService;

    @InjectMocks
    private MediatedRequestLoansController controller;

    @Test
    void checkInByBarcode_shouldReturnCheckInResponse_whenLoanPresent() {
        // given
        CheckInRequest request = new CheckInRequest();
        request.setItemBarcode("123456");
        CheckInResponseLoan loan = new CheckInResponseLoan();
        loan.setId("loan-id");
        CheckInResponse expectedResponse = new CheckInResponse();
        expectedResponse.setLoan(loan);
        when(checkInService.checkIn(any(CheckInRequest.class))).thenReturn(expectedResponse);

        // when
        ResponseEntity<CheckInResponse> response = controller.checkInByBarcode(request);

        // then
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getLoan(), notNullValue());
        assertThat(response.getBody().getLoan().getId(), is("loan-id"));
        verify(checkInService).checkIn(request);
    }

    @Test
    void checkInByBarcode_shouldReturnCheckInResponse_whenLoanAbsent() {
        // given
        CheckInRequest request = new CheckInRequest();
        request.setItemBarcode("123456");
        CheckInResponse expectedResponse = new CheckInResponse();
        expectedResponse.setLoan(null);
        when(checkInService.checkIn(any(CheckInRequest.class))).thenReturn(expectedResponse);

        // when
        ResponseEntity<CheckInResponse> response = controller.checkInByBarcode(request);

        // then
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getLoan(), nullValue());
        verify(checkInService).checkIn(request);
    }
}
