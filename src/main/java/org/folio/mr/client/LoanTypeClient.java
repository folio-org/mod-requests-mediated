package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.LoanType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "loan-types")
public interface LoanTypeClient {

  @GetExchange("/{id}")
  Optional<LoanType> get(@PathVariable String id);
}
