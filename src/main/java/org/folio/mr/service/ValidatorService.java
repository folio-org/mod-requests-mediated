package org.folio.mr.service;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.entity.MediatedRequestEntity;

public interface ValidatorService {
  void validateRequesterForSave(MediatedRequest mediatedRequest);
  void validateRequesterForConfirm(MediatedRequestEntity mediatedRequest);
}
