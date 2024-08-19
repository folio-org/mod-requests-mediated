package org.folio.mr.controller;

import java.util.UUID;

import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequests;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.folio.mr.rest.resource.RequestsMediatedApi;
import org.folio.mr.service.MediatedRequestsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@AllArgsConstructor
public class MediatedRequestsController implements RequestsMediatedApi {

  private final MediatedRequestsService mediatedRequestsService;

  @Override
  public ResponseEntity<MediatedRequest> postMediatedRequest(MediatedRequest mediatedRequest) {
    var storedMediatedRequest = mediatedRequestsService.post(mediatedRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(storedMediatedRequest);
  }

  @Override
  public ResponseEntity<MediatedRequest> getMediatedRequestById(UUID mediatedRequestId) {
    log.debug("getMediatedRequest:: parameters id: {}", mediatedRequestId);
    return mediatedRequestsService.get(mediatedRequestId)
      .map(ResponseEntity.status(HttpStatus.OK)::body)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<MediatedRequests> getMediatedRequestCollection(String query,
    Integer offset, Integer limit) {

    MediatedRequests mediatedRequests;
    if (query == null || query.isEmpty()) {
      mediatedRequests = mediatedRequestsService.findAll(offset, limit);
    } else {
      mediatedRequests = mediatedRequestsService.findBy(query, offset, limit);
    }
    return ResponseEntity.status(HttpStatus.OK).body(mediatedRequests);
  }

  @Override
  public ResponseEntity<Void> putMediatedRequestById(UUID requestId, MediatedRequest mediatedRequest) {
    return mediatedRequestsService.update(requestId, mediatedRequest)
      .map(entity -> ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build())
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> deleteMediatedRequestById(UUID requestId) {
    return mediatedRequestsService.delete(requestId)
      .map(entity -> ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build())
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> confirmItemArrival(ConfirmItemArrivalRequest request) {
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> sendItemInTransit(SendItemInTransitRequest mediatedRequest) {
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
