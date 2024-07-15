package org.folio.mr.service;

import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequests;

public interface MediatedRequestsService {
  Optional<MediatedRequest> get(UUID requestId);

  MediatedRequests findBy(String query, Integer offset, Integer limit);

  MediatedRequest post(MediatedRequest mediatedRequest);

  MediatedRequest update(MediatedRequest mediatedRequest);

  void delete(MediatedRequest mediatedRequest);
}
