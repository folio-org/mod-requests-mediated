package org.folio.mr.service;

import java.util.Optional;

import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.Request;

public interface CirculationStorageService {
  Optional<LoanPolicy> fetchLoanPolicy(String loanPolicyId);
  LoanPolicy createLoanPolicy(LoanPolicy loanPolicy);
  Optional<Request> fetchRequest(String id);
}
