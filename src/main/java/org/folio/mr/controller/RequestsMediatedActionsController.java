package org.folio.mr.controller;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.folio.mr.rest.resource.RequestsMediatedActionsApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@AllArgsConstructor
public class RequestsMediatedActionsController implements RequestsMediatedActionsApi {
  @Override
  public ResponseEntity<Void> confirmItemArrival(ConfirmItemArrivalRequest request) {
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> sendItemInTransit(SendItemInTransitRequest mediatedRequest) {
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
