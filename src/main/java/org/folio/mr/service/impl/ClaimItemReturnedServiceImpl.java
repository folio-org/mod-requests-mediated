package org.folio.mr.service.impl;

import static java.util.Optional.ofNullable;

import java.util.UUID;

import org.folio.mr.client.ClaimItemReturnedCirculationClient;
import org.folio.mr.client.ClaimItemReturnedTlrClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.ClaimItemReturnedService;
import org.folio.mr.service.ConsortiumService;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class ClaimItemReturnedServiceImpl implements ClaimItemReturnedService {

    private final ClaimItemReturnedCirculationClient claimItemReturnedCirculationClient;
    private final ClaimItemReturnedTlrClient claimItemReturnedTlrClient;
    private final MediatedRequestsRepository mediatedRequestsRepository;
    private final LoanClient loanClient;
    private final RequestStorageClient requestStorageClient;
    private final SystemUserScopedExecutionService systemUserService;
    private final ConsortiumService consortiumService;

    @Override
    public void claimItemReturned(UUID loanId, ClaimItemReturnedCirculationRequest request) {
      log.info("claimItemReturned:: loanId={}, itemClaimedReturnedDateTime={}", loanId, request.getItemClaimedReturnedDateTime());
      // Forward request to local mod-circulation
      claimItemReturnedCirculationClient.claimItemReturned(loanId.toString(), request);
      log.info("claimItemReturned:: Claimed item returned locally for loanId: {}", loanId);

      // Find mediated request and fake user ID
      var mediatedRequest = loanClient.getLoanById(loanId.toString())
        .flatMap(loan -> mediatedRequestsRepository.findLastClosedFilled(
            UUID.fromString(loan.getUserId()), loan.getItemId()))
        .orElseThrow(() -> new NotFoundException("Mediated request not found for loanId: " + loanId));

      claimItemReturnedInCentralTenantTlr(mediatedRequest, request);
    }

    private void claimItemReturnedInCentralTenantTlr(MediatedRequestEntity mediatedRequest,
      ClaimItemReturnedCirculationRequest claimItemReturnedRequest) {
      log.info("claimItemReturnedInCentralTenantTlr:: mediatedRequest.id={}, itemClaimedReturnedDateTime={}",
        mediatedRequest::getId, claimItemReturnedRequest::getItemClaimedReturnedDateTime);

      systemUserService.executeAsyncSystemUserScoped(consortiumService.getCentralTenantId(),
        () -> ofNullable(mediatedRequest.getConfirmedRequestId())
          .map(UUID::toString)
          .map(this::fetchRequestFromCentralTenant)
          .map(Request::getRequesterId)
          .ifPresent(fakeRequesterId -> claimItemReturnedInTlr(mediatedRequest.getItemId(),
            fakeRequesterId, claimItemReturnedRequest)));
    }

    private Request fetchRequestFromCentralTenant(String requestId) {
        log.info("fetchRequestFromCentralTenant:: requestId={}", requestId);
        return requestStorageClient.getRequest(requestId)
          .orElseThrow(() -> new NotFoundException("Request not found in Central tenant for ID: "
            + requestId));
    }

    private void claimItemReturnedInTlr(UUID itemId, String fakeRequesterId,
      ClaimItemReturnedCirculationRequest claimItemReturnedRequest) {

      log.info("claimItemReturnedInTlr:: itemId={}, fakeRequesterId={}, itemClaimedReturnedDateTime={}",
        () -> itemId, () -> fakeRequesterId, claimItemReturnedRequest::getItemClaimedReturnedDateTime);

      claimItemReturnedTlrClient.claimItemReturned(new ClaimItemReturnedTlrRequest()
        .itemId(itemId)
        .userId(UUID.fromString(fakeRequesterId))
        .itemClaimedReturnedDateTime(claimItemReturnedRequest.getItemClaimedReturnedDateTime())
        .comment(claimItemReturnedRequest.getComment()));
    }
}
