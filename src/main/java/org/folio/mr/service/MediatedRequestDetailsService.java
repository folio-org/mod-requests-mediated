package org.folio.mr.service;

import java.util.List;

import org.folio.mr.domain.dto.MediatedRequest;

public interface MediatedRequestDetailsService {
  MediatedRequest addRequestDetailsForCreate(MediatedRequest request);
  MediatedRequest addRequestDetailsForUpdate(MediatedRequest request);
  MediatedRequest addRequestDetailsForGet(MediatedRequest request);
  List<MediatedRequest> addRequestBatchDetailsForGet(List<MediatedRequest> requests);
}
