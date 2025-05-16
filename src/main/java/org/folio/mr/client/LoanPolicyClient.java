package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "loan-policies", url = "loan-policy-storage/loan-policies",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface LoanPolicyClient {

  @GetMapping("/{id}")
  Optional<LoanPolicy> get(@PathVariable String id);

  @PostMapping
  LoanPolicy post(@RequestBody LoanPolicy loanPolicy);
}
