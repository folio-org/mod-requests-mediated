package org.folio.mr.service.impl;

import static java.util.Optional.ofNullable;

import java.util.UUID;
import java.util.function.Consumer;

import org.folio.mr.client.LoanClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.ConsortiumService;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.service.SystemUserScopedExecutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public abstract class AbstractMediatedRequestActionService {

    protected final MediatedRequestsRepository mediatedRequestsRepository;
    protected final LoanClient loanClient;
    protected final RequestStorageClient requestStorageClient;
    protected final SystemUserScopedExecutionService systemUserService;
    protected final ConsortiumService consortiumService;

    protected MediatedRequestEntity findMediatedRequest(UUID loanId) {
      var loan = loanClient.getLoanById(loanId.toString())
        .orElseThrow(() -> new NotFoundException("Loan not found for loanId: " + loanId));

      return mediatedRequestsRepository.findLastClosedFilled(
        UUID.fromString(loan.getUserId()), loan.getItemId())
        .orElseThrow(() -> new IllegalArgumentException("Mediated request not found for loanId: " + loanId));
    }

    protected Request fetchRequestLocally(String requestId) {
      log.info("fetchRequestLocally:: requestId={}", requestId);

      return requestStorageClient.getRequest(requestId)
        .orElseThrow(() -> new NotFoundException("Request not found locally for ID: " + requestId));
    }

    protected void executeInCentralTenant(MediatedRequestEntity mediatedRequest, Consumer<String> action) {
      systemUserService.executeAsyncSystemUserScoped(consortiumService.getCentralTenantId(),
        () -> ofNullable(mediatedRequest.getConfirmedRequestId())
          .map(UUID::toString)
          .map(this::fetchRequestLocally)
          .map(Request::getRequesterId)
          .ifPresent(action));
    }
}
