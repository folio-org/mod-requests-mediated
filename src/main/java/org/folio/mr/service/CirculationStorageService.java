package org.folio.mr.service;

import java.util.Optional;

import org.folio.mr.domain.dto.LoanPolicy;

public interface CirculationStorageService {
  Optional<LoanPolicy> fetchLoanPolicy(String loanPolicyId);
  LoanPolicy createLoanPolicy(LoanPolicy loanPolicy);
}
