package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.LoanPolicy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "loan-policy-storage/loan-policies")
public interface LoanPolicyClient {

  @GetExchange("/{id}")
  Optional<LoanPolicy> get(@PathVariable String id);

  @PostExchange
  LoanPolicy post(@RequestBody LoanPolicy loanPolicy);
}
