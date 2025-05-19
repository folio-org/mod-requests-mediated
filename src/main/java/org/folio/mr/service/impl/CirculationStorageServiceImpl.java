package org.folio.mr.service.impl;

import java.util.Optional;

import org.folio.mr.client.LoanPolicyClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.service.CirculationStorageService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationStorageServiceImpl implements CirculationStorageService {

  private final LoanPolicyClient loanPolicyClient;
  private final RequestStorageClient requestStorageClient;

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
}
