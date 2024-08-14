package org.folio.mr.service;

import org.folio.mr.domain.dto.MediatedRequest;

public interface MediatedRequestDetailsService {
  MediatedRequest fetchRequestDetailsForCreation(MediatedRequest request);
  MediatedRequest fetchRequestDetailsForRetrieval(MediatedRequest request);
}
