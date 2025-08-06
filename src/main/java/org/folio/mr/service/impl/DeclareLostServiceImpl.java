package org.folio.mr.service.impl;

import java.util.UUID;

import org.folio.mr.client.DeclareLostCirculationClient;
import org.folio.mr.client.DeclareLostTlrClient;
import org.folio.mr.client.LoanClient;
import org.folio.mr.client.RequestStorageClient;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;
import org.folio.mr.domain.dto.DeclareLostTlrRequest;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.DeclareLostService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class DeclareLostServiceImpl extends AbstractMediatedRequestActionService implements DeclareLostService {

  private final DeclareLostCirculationClient declareLostCirculationClient;
  private final DeclareLostTlrClient declareLostTlrClient;

  public DeclareLostServiceImpl(MediatedRequestsRepository mediatedRequestsRepository,
    LoanClient loanClient, RequestStorageClient requestStorageClient, SystemUserScopedExecutionService systemUserService,
    ConsortiumService consortiumService, DeclareLostCirculationClient declareLostCirculationClient,
    DeclareLostTlrClient declareLostTlrClient) {

    super(mediatedRequestsRepository, loanClient, requestStorageClient, systemUserService, consortiumService);
    this.declareLostCirculationClient = declareLostCirculationClient;
    this.declareLostTlrClient = declareLostTlrClient;
  }

  @Override
  public void declareLost(UUID loanId, DeclareLostCirculationRequest declareLostRequest) {
    log.info("declareLost:: loanId={}, declaredLostDateTime={}, servicePointId={}", () -> loanId,
      declareLostRequest::getDeclaredLostDateTime, declareLostRequest::getServicePointId);

    // Declare item lost locally
    declareLostCirculationClient.declareItemLost(loanId.toString(), declareLostRequest);
    log.info("declareLost:: Declared item lost locally for loanId: {}", loanId);

    // Declare item lost in the central tenant
    var mediatedRequest = findMediatedRequest(loanId);
    executeInCentralTenant(mediatedRequest, fakeRequesterId ->
      declareItemLostInTlr(mediatedRequest.getItemId(), fakeRequesterId, declareLostRequest));
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
