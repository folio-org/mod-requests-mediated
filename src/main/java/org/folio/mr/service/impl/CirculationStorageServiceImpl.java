package org.folio.mr.service.impl;

import java.util.Collection;
import java.util.Optional;

import org.folio.mr.client.LoanClient;
import org.folio.mr.client.LoanPolicyClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.Loan;
import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.service.CirculationStorageService;
import org.folio.mr.support.CqlQuery;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationStorageServiceImpl implements CirculationStorageService {

  private final LoanPolicyClient loanPolicyClient;
  private final LoanClient loanClient;
  private final RequestStorageClient requestStorageClient;
  private static final String LOAN_STATUS_OPEN = "Open";

  @Override
  public Optional<LoanPolicy> fetchLoanPolicy(String loanPolicyId) {
    log.info("resolveLoanPolicy:: fetching loan policy {}", loanPolicyId);
    Optional<LoanPolicy> loanPolicy = loanPolicyClient.get(loanPolicyId);
    log.info("resolveLoanPolicy:: loan policy found: {}", loanPolicy::isPresent);
    return loanPolicy;
  }

  @Override
  public LoanPolicy createLoanPolicy(LoanPolicy loanPolicy) {
    log.info("createLoanPolicy:: creating loan policy {}", loanPolicy::getId);
    return loanPolicyClient.post(loanPolicy);
  }

  @Override
  public Optional<Request> fetchRequest(String id) {
    log.info("fetchRequest:: fetching request {}", id);
    Optional<Request> request = requestStorageClient.getRequest(id);
    log.info("fetchRequest:: request found: {}", request::isPresent);
    return request;
  }

  @Override
  public Collection<Loan> findLoans(CqlQuery query, int limit) {
    log.info("findLoans:: fetching loans by query: {}", query);
    Collection<Loan> loans = loanClient.getByQuery(query, limit)
      .getLoans();
    log.info("findLoans:: found {} loans", loans.size());
    return loans;
  }

  @Override
  public Optional<Loan> findOpenLoan(String itemId) {
    CqlQuery query = CqlQuery.exactMatch("itemId", itemId)
      .and(CqlQuery.exactMatch("status.name", LOAN_STATUS_OPEN));

    return findLoans(query, 1)
      .stream()
      .findFirst();
  }
}
