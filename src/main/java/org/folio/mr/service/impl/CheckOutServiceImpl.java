package org.folio.mr.service.impl;

import static org.folio.mr.exception.ExceptionFactory.notFound;

import java.util.UUID;

import org.folio.mr.client.CheckOutClient;
import org.folio.mr.domain.dto.CheckOutDryRunRequest;
import org.folio.mr.domain.dto.CheckOutDryRunResponse;
import org.folio.mr.domain.dto.CheckOutRequest;
import org.folio.mr.domain.dto.CheckOutResponse;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.LoanPolicy;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.entity.FakePatronLink;
import org.folio.mr.domain.mapper.CirculationMapper;
import org.folio.mr.service.CheckOutService;
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

  private final SystemUserScopedExecutionService systemUserService;
  private final CheckOutClient checkOutClient;
  private final CirculationMapper circulationMapper;
  private final FakePatronLinkService fakePatronLinkService;
  private final UserService userService;
  private final SearchService searchService;
  private final CirculationStorageService circulationStorageService;
  private final ConsortiumService consortiumService;

  @Override
  public CheckOutResponse checkOut(CheckOutRequest request) {
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
    String fakeUserBarcode = resolveFakeUserBarcode(request.getUserBarcode());
    CheckOutDryRunRequest dryRunRequest = circulationMapper.toDryRunRequest(request)
      .userBarcode(fakeUserBarcode);
    CheckOutDryRunResponse dryRunResponse = systemUserService.executeSystemUserScoped(
      lendingTenantId, () -> checkOutDryRun(dryRunRequest));
    String loanPolicyId = dryRunResponse.getLoanPolicyId();
    log.info("resolveLoanPolicy:: resolved loan policy ID: {}", loanPolicyId);
    return loanPolicyId;
  }

  private String resolveFakeUserBarcode(String realUserBarcode) {
    String barcode = userService.fetchUserByBarcode(realUserBarcode)
      .map(User::getId)
      .flatMap(fakePatronLinkService::getFakePatronLink)
      .map(FakePatronLink::getFakeUserId)
      .map(UUID::toString)
      .map(userService::fetchUser)
      .map(User::getBarcode)
      .orElseThrow();

    log.info("resolveFakeUserBarcode:: user barcode is {}", barcode);
    return barcode;
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
    circulationStorageService.createLoanPolicy(loanPolicy);
  }

  private CheckOutResponse doCheckOut(CheckOutRequest request) {
    log.info("checkOut: checking out item {} for user {}", request::getItemBarcode,
      request::getItemBarcode);
    return checkOutClient.checkOut(request);
  }

}
