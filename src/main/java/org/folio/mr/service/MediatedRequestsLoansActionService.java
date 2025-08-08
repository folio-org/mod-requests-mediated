package org.folio.mr.service;

import java.util.UUID;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;
import org.folio.mr.domain.dto.DeclareLostCirculationRequest;

public interface MediatedRequestsLoansActionService {
    void claimItemReturned(UUID loanId, ClaimItemReturnedCirculationRequest request);
    void declareLost(UUID loanId, DeclareLostCirculationRequest declareLostRequest);
}

