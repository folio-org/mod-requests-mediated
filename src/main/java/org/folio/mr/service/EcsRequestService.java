package org.folio.mr.service;

import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.entity.MediatedRequestEntity;

public interface EcsRequestService {
  EcsTlr create(MediatedRequestEntity mediatedRequest);
}
