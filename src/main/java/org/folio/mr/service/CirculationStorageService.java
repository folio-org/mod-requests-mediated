package org.folio.mr.service;

import java.util.Collection;
import java.util.Optional;

import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.support.CqlQuery;

public interface CirculationStorageService {
  Optional<LoanPolicy> fetchLoanPolicy(String loanPolicyId);
  LoanPolicy createLoanPolicy(LoanPolicy loanPolicy);
  Optional<Request> fetchRequest(String id);
  Collection<Loan> findLoans(CqlQuery query, int limit);
  Optional<Loan> findOpenLoan(String itemId);
}
