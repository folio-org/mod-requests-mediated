package org.folio.mr.service.impl;

import static java.util.Optional.ofNullable;

import java.util.UUID;
import java.util.function.Consumer;

import org.folio.mr.client.CirculationErrorForwardingClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.TlrErrorForwardingClient;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingCirculationRequest;
import org.folio.mr.domain.dto.DeclareClaimedReturnedItemAsMissingTlrRequest;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.folio.mr.domain.dto.DeclareLostTlrRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.CirculationStorageService;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.MediatedRequestsLoansActionService;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class MediatedRequestsLoansActionServiceImpl implements MediatedRequestsLoansActionService {

  private final CirculationErrorForwardingClient circulationClient;
  private final TlrErrorForwardingClient tlrClient;

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final LoanClient loanClient;
  private final SystemUserScopedExecutionService systemUserService;
  private final ConsortiumService consortiumService;
  private final CirculationStorageService circulationStorageService;

  @Override
  public void declareLost(UUID loanId, DeclareLostCirculationRequest declareLostRequest) {
    log.info("declareLost:: loanId={}, declaredLostDateTime={}, servicePointId={}", () -> loanId,
      declareLostRequest::getDeclaredLostDateTime, declareLostRequest::getServicePointId);
    circulationClient.declareItemLost(loanId.toString(), declareLostRequest);
    log.info("declareLost:: Declared item lost locally for loanId: {}", loanId);
    var mediatedRequest = findMediatedRequest(loanId);
    executeInCentralTenant(mediatedRequest, fakeRequesterId ->
      declareItemLostInTlr(mediatedRequest.getItemId(), fakeRequesterId, declareLostRequest));
  }

  @Override
  public void claimItemReturned(UUID loanId, ClaimItemReturnedCirculationRequest request) {
    log.info("claimItemReturned:: loanId={}, itemClaimedReturnedDateTime={}", loanId,
      request.getItemClaimedReturnedDateTime());
    circulationClient.claimItemReturned(loanId.toString(), request);
    log.info("claimItemReturned:: Claimed item returned locally for loanId: {}", loanId);
    var mediatedRequest = findMediatedRequest(loanId);
    executeInCentralTenant(mediatedRequest, fakeRequesterId ->
      claimItemReturnedInTlr(mediatedRequest.getItemId(), fakeRequesterId, request));
  }

  @Override
  public void declareItemMissing(UUID loanId,
    DeclareClaimedReturnedItemAsMissingCirculationRequest request) {

    log.info("declareItemMissing:: loanId={}", loanId);
    circulationClient.declareClaimedReturnedItemAsMissing(loanId.toString(), request);
    log.info("declareItemMissing:: declared item missing locally for loanId: {}", loanId);
    var mediatedRequest = findMediatedRequest(loanId);
    executeInCentralTenant(mediatedRequest, fakeRequesterId ->
      declareItemMissingInTlr(mediatedRequest.getItemId(), fakeRequesterId, request));
  }

  private MediatedRequestEntity findMediatedRequest(UUID loanId) {
    var loan = loanClient.getLoanById(loanId.toString())
      .orElseThrow(() -> new NotFoundException("Loan not found for loanId: " + loanId));
    return mediatedRequestsRepository.findLastClosedFilled(
      UUID.fromString(loan.getUserId()), loan.getItemId())
      .orElseThrow(() -> new NotFoundException("Mediated request not found for loanId: " + loanId));
    //TODO handle this case with ValidationException
  }

  private Request fetchRequestLocally(String requestId) {
    log.info("fetchRequestLocally:: requestId={}", requestId);
    return circulationStorageService.fetchRequest(requestId)
      .orElseThrow(() -> new NotFoundException("Request not found locally for ID: " + requestId));
  }

  private void executeInCentralTenant(MediatedRequestEntity mediatedRequest, Consumer<String> action) {
    systemUserService.executeAsyncSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> ofNullable(mediatedRequest.getConfirmedRequestId())
        .map(UUID::toString)
        .map(this::fetchRequestLocally)
        .map(Request::getRequesterId)
        .ifPresent(action));
  }

  private void claimItemReturnedInTlr(UUID itemId, String fakeRequesterId,
    ClaimItemReturnedCirculationRequest claimItemReturnedRequest) {
    log.info("claimItemReturnedInTlr:: itemId={}, fakeRequesterId={}, itemClaimedReturnedDateTime={}",
      () -> itemId, () -> fakeRequesterId, claimItemReturnedRequest::getItemClaimedReturnedDateTime);
    tlrClient.claimItemReturned(
      new ClaimItemReturnedTlrRequest()
        .itemId(itemId)
        .userId(UUID.fromString(fakeRequesterId))
        .itemClaimedReturnedDateTime(claimItemReturnedRequest.getItemClaimedReturnedDateTime())
        .comment(claimItemReturnedRequest.getComment())
    );
  }

  private void declareItemLostInTlr(UUID itemId, String fakeRequesterId, DeclareLostCirculationRequest declareLostRequest) {
    log.info("declareItemLostInTlr:: itemId={}, fakeRequesterId={}, declaredLostDateTime={}, servicePointId={}",
      () -> itemId, () -> fakeRequesterId, declareLostRequest::getDeclaredLostDateTime, declareLostRequest::getServicePointId);
    tlrClient.declareItemLost(
      new DeclareLostTlrRequest()
        .itemId(itemId)
        .userId(UUID.fromString(fakeRequesterId))
        .servicePointId(declareLostRequest.getServicePointId())
        .declaredLostDateTime(declareLostRequest.getDeclaredLostDateTime())
        .comment(declareLostRequest.getComment())
    );
  }

  private void declareItemMissingInTlr(UUID itemId, String fakeRequesterId,
    DeclareClaimedReturnedItemAsMissingCirculationRequest request) {

    log.info("declareItemMissingInTlr:: itemId={}, fakeRequesterId={}", itemId, fakeRequesterId);
    tlrClient.declareClaimedReturnedItemAsMissing(
      new DeclareClaimedReturnedItemAsMissingTlrRequest()
        .itemId(itemId)
        .userId(UUID.fromString(fakeRequesterId))
        .comment(request.getComment())
    );
  }
}
