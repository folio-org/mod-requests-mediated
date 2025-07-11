package org.folio.mr.service;

import java.util.UUID;

import org.folio.mr.domain.dto.DeclareLostCirculationRequest;

public interface DeclareLostService {
  void declareLost(UUID loanId, DeclareLostCirculationRequest declareLostRequest);
}
