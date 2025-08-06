package org.folio.mr.service.impl;

import java.util.UUID;

import org.folio.mr.client.ClaimItemReturnedCirculationClient;
import org.folio.mr.client.ClaimItemReturnedTlrClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.ClaimItemReturnedTlrRequest;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.ClaimItemReturnedService;
import org.folio.mr.service.ConsortiumService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ClaimItemReturnedServiceImpl extends AbstractMediatedRequestActionService implements ClaimItemReturnedService {

    private final ClaimItemReturnedCirculationClient claimItemReturnedCirculationClient;
    private final ClaimItemReturnedTlrClient claimItemReturnedTlrClient;

    public ClaimItemReturnedServiceImpl(MediatedRequestsRepository mediatedRequestsRepository,
      LoanClient loanClient, RequestStorageClient requestStorageClient,
      SystemUserScopedExecutionService systemUserService, ConsortiumService consortiumService,
      ClaimItemReturnedCirculationClient claimItemReturnedCirculationClient,
      ClaimItemReturnedTlrClient claimItemReturnedTlrClient) {

      super(mediatedRequestsRepository, loanClient, requestStorageClient, systemUserService, consortiumService);
      this.claimItemReturnedCirculationClient = claimItemReturnedCirculationClient;
      this.claimItemReturnedTlrClient = claimItemReturnedTlrClient;
    }

    @Override
    public void claimItemReturned(UUID loanId, ClaimItemReturnedCirculationRequest request) {
      log.info("claimItemReturned:: loanId={}, itemClaimedReturnedDateTime={}", loanId, request.getItemClaimedReturnedDateTime());
      // 1. Forward request to local mod-circulation
      claimItemReturnedCirculationClient.claimItemReturned(loanId.toString(), request);
      log.info("claimItemReturned:: Claimed item returned locally for loanId: {}", loanId);

      // 2. Find mediated request and fake user ID, then send to TLR
      var mediatedRequest = findMediatedRequest(loanId);
      executeInCentralTenant(mediatedRequest, fakeRequesterId -> claimItemReturnedInTlr(
        mediatedRequest.getItemId(), fakeRequesterId, request));
    }

    private void claimItemReturnedInTlr(UUID itemId, String fakeRequesterId,
      ClaimItemReturnedCirculationRequest claimItemReturnedRequest) {

      log.info("claimItemReturnedInTlr:: itemId={}, fakeRequesterId={}, itemClaimedReturnedDateTime={}",
        () -> itemId, () -> fakeRequesterId,claimItemReturnedRequest::getItemClaimedReturnedDateTime);

        claimItemReturnedTlrClient.claimItemReturned(
          new ClaimItemReturnedTlrRequest()
            .itemId(itemId)
            .userId(UUID.fromString(fakeRequesterId))
            .itemClaimedReturnedDateTime(claimItemReturnedRequest.getItemClaimedReturnedDateTime())
            .comment(claimItemReturnedRequest.getComment())
        );
    }
}
