package org.folio.mr.service.impl;

import static java.util.Optional.ofNullable;

import java.util.UUID;

import org.folio.mr.client.DeclareLostCirculationClient;
import org.folio.mr.client.DeclareLostTlrClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.folio.mr.domain.dto.DeclareLostTlrRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.DeclareLostService;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class DeclareLostServiceImpl implements DeclareLostService {

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final LoanClient loanClient;
  private final RequestStorageClient requestStorageClient;
  private final SystemUserScopedExecutionService systemUserService;
  private final ConsortiumService consortiumService;
  private final DeclareLostCirculationClient declareLostCirculationClient;
  private final DeclareLostTlrClient declareLostTlrClient;

  @Override
  public void declareLost(UUID loanId, DeclareLostCirculationRequest declareLostRequest) {
    log.info("declareLost:: loanId={}, declaredLostDateTime={}, servicePointId={}", () -> loanId,
      declareLostRequest::getDeclaredLostDateTime, declareLostRequest::getServicePointId);

    // Declare item lost locally
    declareLostCirculationClient.declareItemLost(loanId.toString(), declareLostRequest);
    log.info("declareLost:: Declared item lost locally for loanId: {}", loanId);

    // Declare item lost in the central tenant

    var mediatedRequest = loanClient.getLoanById(loanId.toString())
      .flatMap(loan -> mediatedRequestsRepository.findLastClosedFilled(
        UUID.fromString(loan.getUserId()), loan.getItemId()))
      .orElseThrow(() -> new NotFoundException("Mediated request not found for loanId: " + loanId));

    declareItemLostInCentralTenantTlr(mediatedRequest, declareLostRequest);
  }

  private void declareItemLostInCentralTenantTlr(MediatedRequestEntity mediatedRequest,
    DeclareLostCirculationRequest declareLostRequest) {

    log.info("declareItemLostInCentralTenantTlr:: mediatedRequest.id={}, " +
        "declaredLostDateTime={}, servicePointId={}", mediatedRequest::getId,
      declareLostRequest::getDeclaredLostDateTime, declareLostRequest::getServicePointId);

    systemUserService.executeAsyncSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> ofNullable(mediatedRequest.getConfirmedRequestId())
        .map(UUID::toString)
        .map(this::fetchRequestFromCentralTenant)
        .map(Request::getRequesterId)
        .ifPresent(fakeRequesterId -> declareItemLostInTlr(mediatedRequest.getItemId(),
          fakeRequesterId, declareLostRequest)));
  }

  private Request fetchRequestFromCentralTenant(String requestId) {
    log.info("fetchRequestFromCentralTenant:: requestId={}", requestId);

    return requestStorageClient.getRequest(requestId)
      .orElseThrow(() -> new NotFoundException(
        "Request not found in Central tenant for ID: " + requestId));
  }

  private void declareItemLostInTlr(UUID itemId, String fakeRequesterId,
    DeclareLostCirculationRequest declareLostRequest) {

    log.info("declareItemLostInTlr:: itemId={}, fakeRequesterId={}, declaredLostDateTime={}, " +
        "servicePointId={}", () -> itemId,
      () -> fakeRequesterId, declareLostRequest::getDeclaredLostDateTime,
      declareLostRequest::getServicePointId);

    declareLostTlrClient.declareItemLost(
      new DeclareLostTlrRequest()
        .itemId(itemId)
        .userId(UUID.fromString(fakeRequesterId))
        .servicePointId(declareLostRequest.getServicePointId())
        .comment(declareLostRequest.getComment()));
  }
}
