package org.folio.mr.controller;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.SecureRequest;
import org.folio.mr.rest.resource.RequestsMediatedApi;
import org.folio.mr.service.SecureRequestsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@Log4j2
@AllArgsConstructor
public class SecureRequestsController implements RequestsMediatedApi {

  private final SecureRequestsService secureRequestsService;

  @Override
  public ResponseEntity<SecureRequest> get(UUID requestId) {
    log.debug("get:: parameters id: {}", requestId);
    return secureRequestsService.get(requestId)
      .map(ResponseEntity.status(HttpStatus.OK)::body)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
