package org.folio.mr.service;

import java.util.UUID;
import org.folio.mr.domain.dto.ClaimItemReturnedCirculationRequest;

public interface ClaimItemReturnedService {
    void claimItemReturned(UUID loanId, ClaimItemReturnedCirculationRequest request);
}

