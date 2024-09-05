package org.folio.mr.service;

import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;

public interface CirculationRequestService {
  Request create(MediatedRequestEntity mediatedRequest);
}
