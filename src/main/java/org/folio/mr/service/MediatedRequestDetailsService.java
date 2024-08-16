package org.folio.mr.service;

import org.folio.mr.domain.dto.MediatedRequest;

public interface MediatedRequestDetailsService {
  MediatedRequest populateRequestDetailsForCreate(MediatedRequest request);
  MediatedRequest populateRequestDetailsForUpdate(MediatedRequest request);
  MediatedRequest populateRequestDetailsForGet(MediatedRequest request);
}
