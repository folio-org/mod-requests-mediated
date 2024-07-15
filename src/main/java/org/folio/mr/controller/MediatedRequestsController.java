package org.folio.mr.controller;

import java.util.Set;
import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequests;
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
  public ResponseEntity<MediatedRequest> getMediatedRequestById(UUID requestId) {
    log.debug("getMediatedRequest:: parameters id: {}", requestId);
    return mediatedRequestsService.get(requestId)
      .map(ResponseEntity.status(HttpStatus.OK)::body)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<MediatedRequests> getMediatedRequestCollection(String query,
    Integer offset, Integer limit) {

    var mediatedRequests = mediatedRequestsService.findBy(query, offset, limit);
    return ResponseEntity.status(HttpStatus.OK).body(mediatedRequests);
  }

  @Override
  public ResponseEntity<Void> putMediatedRequestById(UUID requestId, MediatedRequest mediatedRequest) {
    return RequestsMediatedApi.super.putMediatedRequestById(requestId, mediatedRequest);
  }

  @Override
  public ResponseEntity<Void> deleteMediatedRequestById(UUID requestId) {
    return RequestsMediatedApi.super.deleteMediatedRequestById(requestId);
  }

}
