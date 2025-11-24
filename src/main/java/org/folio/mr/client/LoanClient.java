package org.folio.mr.client;

import java.util.Optional;

import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.Loans;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "loan-storage", url = "loan-storage/loans",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface LoanClient extends GetByQueryParamsClient<Loans> {

  @GetMapping("/{loanId}")
  Optional<Loan> getLoanById(@PathVariable("loanId") String loanId);

}
