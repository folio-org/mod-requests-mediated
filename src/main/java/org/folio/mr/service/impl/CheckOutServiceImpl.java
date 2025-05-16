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
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.CirculationStorageService;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.FakePatronLinkService;
import org.folio.mr.service.SearchService;
import org.folio.mr.service.UserService;
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
  private final FakePatronLinkService fakePatronLinkService;
  private final UserService userService;
  private final SearchService searchService;
  private final CirculationStorageService circulationStorageService;
  private final ConsortiumService consortiumService;
  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final CirculationRequestService circulationRequestService;

  @Override
  public CheckOutResponse checkOut(CheckOutRequest request) {
    log.info("checkOut:: userBarcode={}, itemBarcode={}", request::getUserBarcode, request::getItemBarcode);
    String lendingTenantId = findItem(request.getItemBarcode()).getTenantId();
    if (!consortiumService.getCurrentTenantId().equals(lendingTenantId)) {
      String loanPolicyId = resolveLoanPolicyId(request, lendingTenantId);
      cloneLoanPolicyToLocalTenant(loanPolicyId, lendingTenantId);
      request.setForceLoanPolicyId(UUID.fromString(loanPolicyId));
    }
    return doCheckOut(request);
  }

  private ConsortiumItem findItem(String itemBarcode) {
    return searchService.searchItemByBarcode(itemBarcode)
      .orElseThrow(() -> notFound("Failed to find item with barcode " + itemBarcode));
  }

  private String resolveLoanPolicyId(CheckOutRequest request, String lendingTenantId) {
    log.info("resolveLoanPolicy:: resolving loan policy for user {} and item {} in tenant {}",
      request::getUserBarcode, request::getItemBarcode, () -> lendingTenantId);
    String fakeUserBarcode = resolveFakeRequesterBarcode(request, lendingTenantId);
    CheckOutDryRunRequest dryRunRequest = circulationMapper.toDryRunRequest(request)
      .userBarcode(fakeUserBarcode);
    CheckOutDryRunResponse dryRunResponse = systemUserService.executeSystemUserScoped(
      lendingTenantId, () -> checkOutDryRun(dryRunRequest));
    String loanPolicyId = dryRunResponse.getLoanPolicyId();
    log.info("resolveLoanPolicy:: resolved loan policy ID: {}", loanPolicyId);
    return loanPolicyId;
  }

  private String resolveFakeRequesterBarcode(CheckOutRequest request, String lendingTenantId) {
    MediatedRequestEntity mediatedRequest = findMediatedRequest(request);
    String confirmedRequestId = mediatedRequest.getConfirmedRequestId().toString();
    log.info("resolveFakeRequesterBarcode:: mediated request found: {}, confirmed request ID: {}",
      mediatedRequest.getId(), confirmedRequestId);

    // corresponding circulation request in lending tenant has the same ID
    Request requestInLendingTenant = systemUserService.executeSystemUserScoped(lendingTenantId,
      () -> circulationRequestService.get(confirmedRequestId));

    log.info("resolveFakeRequesterBarcode:: request {} in tenant {} found",
      confirmedRequestId, lendingTenantId);

    String fakePatronBarcode = Optional.of(requestInLendingTenant)
      .map(Request::getRequester)
      .map(RequestRequester::getBarcode)
      .orElseThrow();

    log.info("resolveFakeRequesterBarcode:: fake requester barcode found");
    return fakePatronBarcode;
  }

  private MediatedRequestEntity findMediatedRequest(CheckOutRequest request) {
    log.info("findMediatedRequest:: looking for mediated request awaiting pickup/delivery");
    return mediatedRequestsRepository.findRequestForCheckOut(
        request.getItemBarcode(), request.getUserBarcode())
      .orElseThrow(() -> notFound("Failed to find request for check-out"));
  }

  public CheckOutDryRunResponse checkOutDryRun(CheckOutDryRunRequest request) {
    log.info("checkOutDryRun:: check-out dry run for user {} and item {}",
      request::getUserBarcode, request::getItemBarcode);
    return checkOutClient.checkOutDryRun(request);
  }

  private void cloneLoanPolicyToLocalTenant(String loanPolicyId, String lendingTenantId) {
    circulationStorageService.fetchLoanPolicy(loanPolicyId).ifPresentOrElse(
      policy -> log.info("resolveLoanPolicy:: loan policy already exists in local tenant"),
      () -> cloneLoanPolicy(loanPolicyId, lendingTenantId));
  }

  private void cloneLoanPolicy(String loanPolicyId, String sourceTenantId) {
    log.info("cloneLoanPolicy:: fetching loan policy {} from tenant {}", loanPolicyId, sourceTenantId);
    LoanPolicy loanPolicy = systemUserService.executeSystemUserScoped(sourceTenantId,
        () -> circulationStorageService.fetchLoanPolicy(loanPolicyId))
      .orElseThrow(() -> notFound(String.format("Loan policy %s not found in tenant %s",
        loanPolicyId, sourceTenantId)));
    log.info("cloneLoanPolicy:: cloning loan policy {} to local tenant", loanPolicyId);
    loanPolicy.setName(CLONED_LOAN_POLICY_PREFIX + loanPolicy.getName());
    circulationStorageService.createLoanPolicy(loanPolicy);
  }

  private CheckOutResponse doCheckOut(CheckOutRequest request) {
    log.info("checkOut: checking out item {} for user {}", request::getItemBarcode,
      request::getUserBarcode);
    return checkOutClient.checkOut(request);
  }

}
