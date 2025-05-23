package org.folio.mr.service.impl;

import static org.folio.mr.exception.ExceptionFactory.notFound;

import java.util.Optional;
import java.util.UUID;

import org.folio.mr.client.CheckOutClient;
import org.folio.mr.domain.dto.CheckOutDryRunRequest;
import org.folio.mr.domain.dto.CheckOutDryRunResponse;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.RequestRequester;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.mapper.CirculationMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.CheckOutService;
import org.folio.mr.service.CirculationStorageService;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.SearchService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CheckOutServiceImpl implements CheckOutService {

  private static final String CLONED_LOAN_POLICY_PREFIX = "COPY_OF_";

  private final SystemUserScopedExecutionService systemUserService;
  private final CheckOutClient checkOutClient;
  private final CirculationMapper circulationMapper;
  private final SearchService searchService;
  private final CirculationStorageService circulationStorageService;
  private final ConsortiumService consortiumService;
  private final MediatedRequestsRepository mediatedRequestsRepository;

  @Override
  public CheckOutResponse checkOut(CheckOutRequest request) {
    log.info("checkOut:: userBarcode={}, itemBarcode={}", request::getUserBarcode, request::getItemBarcode);
    Optional<MediatedRequestEntity> mediatedRequest = findMediatedRequest(request);
    if (mediatedRequest.isPresent()) {
      log.info("checkOut:: mediated request is found");
      String lendingTenantId = findItem(request.getItemBarcode()).getTenantId();
      if (consortiumService.getCurrentTenantId().equals(lendingTenantId)) {
        log.info("checkOut:: item was found in local tenant, no need to clone loan policy");
      } else {
        log.info("checkOut:: attempting to copy loan policy from lending to local tenant");
        String loanPolicyId = resolveLoanPolicyId(request, lendingTenantId, mediatedRequest.get());
        cloneLoanPolicyToLocalTenant(loanPolicyId, lendingTenantId);
        request.setForceLoanPolicyId(UUID.fromString(loanPolicyId));
      }
    } else {
      log.info("checkOut:: mediated request was not found");
    }
    return doCheckOut(request);
  }

  private ConsortiumItem findItem(String itemBarcode) {
    return searchService.searchItemByBarcode(itemBarcode)
      .orElseThrow(() -> notFound("Failed to find item with barcode " + itemBarcode));
  }

  private Optional<MediatedRequestEntity> findMediatedRequest(CheckOutRequest request) {
    log.info("findMediatedRequest:: looking for mediated request awaiting pickup/delivery");
    return mediatedRequestsRepository.findRequestForCheckOut(
      request.getItemBarcode(), request.getUserBarcode());
  }

  private String resolveLoanPolicyId(CheckOutRequest request, String lendingTenantId,
    MediatedRequestEntity mediatedRequest) {

    log.info("resolveLoanPolicy:: resolving loan policy for user {} and item {} in tenant {}",
      request::getUserBarcode, request::getItemBarcode, () -> lendingTenantId);
    String fakeUserBarcode = resolveFakeRequesterBarcode(mediatedRequest, lendingTenantId);
    CheckOutDryRunRequest dryRunRequest = circulationMapper.toDryRunRequest(request)
      .userBarcode(fakeUserBarcode);
    String loanPolicyId = systemUserService.executeSystemUserScoped(
      lendingTenantId, () -> checkOutDryRun(dryRunRequest).getLoanPolicyId());
    log.info("resolveLoanPolicy:: resolved loan policy ID: {}", loanPolicyId);
    return loanPolicyId;
  }

  private String resolveFakeRequesterBarcode(MediatedRequestEntity mediatedRequest,
    String lendingTenantId) {

    String confirmedRequestId = mediatedRequest.getConfirmedRequestId().toString();
    log.info("resolveFakeRequesterBarcode:: mediated request found: {}, confirmed request ID: {}",
      mediatedRequest.getId(), confirmedRequestId);

    // corresponding request in lending tenant should have the same ID as confirmed request
    Request requestInLendingTenant = systemUserService.executeSystemUserScoped(lendingTenantId,
      () -> circulationStorageService.fetchRequest(confirmedRequestId))
      .orElseThrow(() -> notFound("Failed to find request " + confirmedRequestId));

    log.info("resolveFakeRequesterBarcode:: request {} found in tenant {}",
      confirmedRequestId, lendingTenantId);

    String fakePatronBarcode = Optional.of(requestInLendingTenant)
      .map(Request::getRequester)
      .map(RequestRequester::getBarcode)
      .orElseThrow();

    log.info("resolveFakeRequesterBarcode:: fake requester barcode found");
    return fakePatronBarcode;
  }

  public CheckOutDryRunResponse checkOutDryRun(CheckOutDryRunRequest request) {
    log.info("checkOutDryRun:: check-out dry run for item {}", request::getItemBarcode);
    return checkOutClient.checkOutDryRun(request);
  }

  private void cloneLoanPolicyToLocalTenant(String loanPolicyId, String lendingTenantId) {
    circulationStorageService.fetchLoanPolicy(loanPolicyId).ifPresentOrElse(
      policy -> log.info("resolveLoanPolicy:: loan policy already exists in local tenant"),
      () -> cloneLoanPolicy(loanPolicyId, lendingTenantId));
  }

  private void cloneLoanPolicy(String loanPolicyId, String lendingTenantId) {
    log.info("cloneLoanPolicy:: fetching loan policy {} from lending tenant", loanPolicyId);
    LoanPolicy loanPolicy = systemUserService.executeSystemUserScoped(lendingTenantId,
        () -> circulationStorageService.fetchLoanPolicy(loanPolicyId))
      .orElseThrow(() -> notFound(String.format("Loan policy %s not found in tenant %s",
        loanPolicyId, lendingTenantId)));
    log.info("cloneLoanPolicy:: cloning loan policy {} to local tenant", loanPolicyId);
    loanPolicy.setName(CLONED_LOAN_POLICY_PREFIX + loanPolicy.getName());
    circulationStorageService.createLoanPolicy(loanPolicy);
  }

  private CheckOutResponse doCheckOut(CheckOutRequest request) {
    log.info("checkOut: checking out item {} to user {}", request::getItemBarcode,
      request::getUserBarcode);
    return checkOutClient.checkOut(request);
  }

}
