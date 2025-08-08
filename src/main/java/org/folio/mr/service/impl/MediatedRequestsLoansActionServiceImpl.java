package org.folio.mr.service.impl;

import static java.util.Optional.ofNullable;

import java.util.UUID;
import java.util.function.Consumer;

import org.folio.mr.client.ClaimItemReturnedCirculationClient;
import org.folio.mr.client.ClaimItemReturnedTlrClient;
import org.folio.mr.client.DeclareLostCirculationClient;
import org.folio.mr.client.DeclareLostTlrClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.folio.mr.domain.dto.DeclareLostTlrRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.MediatedRequestsLoansActionService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class MediatedRequestsLoansActionServiceImpl implements MediatedRequestsLoansActionService {

  private final ClaimItemReturnedCirculationClient claimItemReturnedCirculationClient;
  private final ClaimItemReturnedTlrClient claimItemReturnedTlrClient;
  private final DeclareLostCirculationClient declareLostCirculationClient;
  private final DeclareLostTlrClient declareLostTlrClient;

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final LoanClient loanClient;
  private final RequestStorageClient requestStorageClient;
  private final SystemUserScopedExecutionService systemUserService;
  private final ConsortiumService consortiumService;

  private MediatedRequestEntity findMediatedRequest(UUID loanId) {
    var loan = loanClient.getLoanById(loanId.toString())
      .orElseThrow(() -> new org.folio.spring.exception.NotFoundException("Loan not found for loanId: " + loanId));
    return mediatedRequestsRepository.findLastClosedFilled(
      UUID.fromString(loan.getUserId()), loan.getItemId())
      .orElseThrow(() -> new IllegalArgumentException("Mediated request not found for loanId: " + loanId));
  }

  private Request fetchRequestLocally(String requestId) {
    log.info("fetchRequestLocally:: requestId={}", requestId);
    return requestStorageClient.getRequest(requestId)
      .orElseThrow(() -> new org.folio.spring.exception.NotFoundException("Request not found locally for ID: " + requestId));
  }

  private void executeInCentralTenant(MediatedRequestEntity mediatedRequest, Consumer<String> action) {
    systemUserService.executeAsyncSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> ofNullable(mediatedRequest.getConfirmedRequestId())
        .map(UUID::toString)
        .map(this::fetchRequestLocally)
        .map(Request::getRequesterId)
        .ifPresent(action));
  }

  @Override
  public void claimItemReturned(UUID loanId, ClaimItemReturnedCirculationRequest request) {
    log.info("claimItemReturned:: loanId={}, itemClaimedReturnedDateTime={}", loanId, request.getItemClaimedReturnedDateTime());
    claimItemReturnedCirculationClient.claimItemReturned(loanId.toString(), request);
    log.info("claimItemReturned:: Claimed item returned locally for loanId: {}", loanId);
    var mediatedRequest = findMediatedRequest(loanId);
    executeInCentralTenant(mediatedRequest, fakeRequesterId ->
      claimItemReturnedInTlr(mediatedRequest.getItemId(), fakeRequesterId, request));
  }

  private void claimItemReturnedInTlr(UUID itemId, String fakeRequesterId, ClaimItemReturnedCirculationRequest claimItemReturnedRequest) {
    log.info("claimItemReturnedInTlr:: itemId={}, fakeRequesterId={}, itemClaimedReturnedDateTime={}",
      () -> itemId, () -> fakeRequesterId, claimItemReturnedRequest::getItemClaimedReturnedDateTime);
    claimItemReturnedTlrClient.claimItemReturned(
      new ClaimItemReturnedTlrRequest()
        .itemId(itemId)
        .userId(UUID.fromString(fakeRequesterId))
        .itemClaimedReturnedDateTime(claimItemReturnedRequest.getItemClaimedReturnedDateTime())
        .comment(claimItemReturnedRequest.getComment())
    );
  }

  @Override
  public void declareLost(UUID loanId, DeclareLostCirculationRequest declareLostRequest) {
    log.info("declareLost:: loanId={}, declaredLostDateTime={}, servicePointId={}", () -> loanId,
      declareLostRequest::getDeclaredLostDateTime, declareLostRequest::getServicePointId);
    declareLostCirculationClient.declareItemLost(loanId.toString(), declareLostRequest);
    log.info("declareLost:: Declared item lost locally for loanId: {}", loanId);
    var mediatedRequest = findMediatedRequest(loanId);
    executeInCentralTenant(mediatedRequest, fakeRequesterId ->
      declareItemLostInTlr(mediatedRequest.getItemId(), fakeRequesterId, declareLostRequest));
  }

  private void declareItemLostInTlr(UUID itemId, String fakeRequesterId, DeclareLostCirculationRequest declareLostRequest) {
    log.info("declareItemLostInTlr:: itemId={}, fakeRequesterId={}, declaredLostDateTime={}, servicePointId={}",
      () -> itemId, () -> fakeRequesterId, declareLostRequest::getDeclaredLostDateTime, declareLostRequest::getServicePointId);
    declareLostTlrClient.declareItemLost(
      new DeclareLostTlrRequest()
        .itemId(itemId)
        .userId(UUID.fromString(fakeRequesterId))
        .servicePointId(declareLostRequest.getServicePointId())
        .comment(declareLostRequest.getComment())
    );
  }
}
