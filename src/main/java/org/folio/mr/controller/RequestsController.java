package org.folio.mr.controller;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.rest.resource.RequestsApi;
import org.folio.mr.service.RequestsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import static java.util.Objects.isNull;

@RestController
@Log4j2
@AllArgsConstructor
public class RequestsController implements RequestsApi {

  private final RequestsService requestsService;

  @Override
  public ResponseEntity<Request> retrieveRequestById(String id) {
    log.info("retrieveMediatedRequestById: by id= {}", id);
    var request = requestsService.retrieveMediatedRequestById(id);
    return isNull(request) ?
      ResponseEntity.notFound().build() :
      ResponseEntity.status(HttpStatus.OK)
        .body(request);
  }
}
