package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.MaterialType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "material-types", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface MaterialTypeClient {

  @GetExchange("/{id}")
  Optional<MaterialType> get(@PathVariable String id);
}
