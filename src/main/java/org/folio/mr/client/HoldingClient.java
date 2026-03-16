package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.HoldingsRecord;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "holdings-storage/holdings", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface HoldingClient {

  @GetExchange("/{id}")
  Optional<HoldingsRecord> get(@PathVariable String id);
}
