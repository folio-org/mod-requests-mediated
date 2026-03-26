package org.folio.mr.controller;

import java.util.UUID;

import org.folio.mr.domain.dto.CheckInRequest;
import org.folio.mr.domain.dto.CheckInResponse;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingCirculationRequest;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.folio.mr.rest.resource.MediatedRequestsLoansApi;
import org.folio.mr.service.CheckInService;
import org.folio.mr.service.CheckOutService;
import org.folio.mr.service.MediatedRequestsLoansActionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@AllArgsConstructor
public class MediatedRequestLoansController implements MediatedRequestsLoansApi {

  private final CheckInService checkInService;
  private final CheckOutService checkOutService;
  private final MediatedRequestsLoansActionService mediatedRequestsLoansActionService;

  @Override
  public ResponseEntity<CheckOutResponse> checkOutByBarcode(CheckOutRequest request) {
    return ResponseEntity.ok(checkOutService.checkOut(request));
  }

  @Override
  public ResponseEntity<CheckInResponse> checkInByBarcode(CheckInRequest request) {
    return ResponseEntity.ok(checkInService.checkIn(request));
  }

  @Override
  public ResponseEntity<Void> declareItemLost(UUID loanId,
    DeclareLostCirculationRequest declareLostRequest) {

    mediatedRequestsLoansActionService.declareLost(loanId, declareLostRequest);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> claimItemReturned(UUID loanId,
    ClaimItemReturnedCirculationRequest claimItemReturnedRequest) {
    mediatedRequestsLoansActionService.claimItemReturned(loanId, claimItemReturnedRequest);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> declareClaimedReturnedItemAsMissing(UUID loanId,
    DeclareClaimedReturnedItemAsMissingCirculationRequest request) {

    mediatedRequestsLoansActionService.declareItemMissing(loanId, request);
    return ResponseEntity.noContent().build();
  }

  @ExceptionHandler(HttpStatusCodeException.class)
  public ResponseEntity<String> handleHttpStatusCodeException(HttpStatusCodeException e) {
    log.warn("handleHttpStatusCodeException:: forwarding error response with status {}", e.getStatusCode().value());
    return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
  }
}
