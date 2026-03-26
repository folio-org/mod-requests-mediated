package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Loan;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "loan-storage/loans", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LoanClient {

  @GetExchange("/{loanId}")
  Optional<Loan> getLoanById(@PathVariable("loanId") String loanId);
}
