package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Request;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "request-storage/requests", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface RequestStorageClient {

  @GetExchange("/{id}")
  Optional<Request> getRequest(@PathVariable String id);
}
