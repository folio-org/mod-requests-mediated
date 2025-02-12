package org.folio.mr.service;

import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;

public interface CirculationRequestService {
  Request get(String id);

  Request create(MediatedRequestEntity mediatedRequest);

  Request update(Request request);
}
