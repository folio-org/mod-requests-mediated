package org.folio.mr.service;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.entity.MediatedRequestWorkflowLog;

public interface MediatedRequestActionsService {
  MediatedRequest confirmItemArrival(String itemBarcode);

  MediatedRequest sendItemInTransit(String itemBarcode);

  MediatedRequestWorkflowLog saveMediatedRequestWorkflowLog(MediatedRequest request);
}
