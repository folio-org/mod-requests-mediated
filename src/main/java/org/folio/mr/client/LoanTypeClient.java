package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.LoanType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "loan-types", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LoanTypeClient {

  @GetExchange("/{id}")
  Optional<LoanType> get(@PathVariable String id);
}
