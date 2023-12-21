package org.folio.mr.controller;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.SecureRequest;
import org.folio.mr.rest.resource.SecureRequestsApi;
import org.folio.mr.service.SecureRequestsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import static java.util.Objects.isNull;

@RestController
@Log4j2
@AllArgsConstructor
public class SecureRequestsController implements SecureRequestsApi {

  private final SecureRequestsService secureRequestsService;

  @Override
  public ResponseEntity<SecureRequest> retrieveSecureRequestById(String id) {
    log.info("retrieveMediatedRequestById: by id= {}", id);
    var request = secureRequestsService.retrieveMediatedRequestById(id);
    return isNull(request) ?
      ResponseEntity.notFound().build() :
      ResponseEntity.status(HttpStatus.OK)
        .body(request);
  }
}
