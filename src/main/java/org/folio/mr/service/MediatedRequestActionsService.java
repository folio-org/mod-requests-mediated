package org.folio.mr.service;

import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.entity.MediatedRequestWorkflowLog;

public interface MediatedRequestActionsService {
  MediatedRequest confirmItemArrival(String itemBarcode);
  MediatedRequest sendItemInTransit(String itemBarcode);
  void confirm(UUID mediatedRequestId);
  void decline(UUID mediatedRequestId);
  MediatedRequestWorkflowLog saveMediatedRequestWorkflowLog(MediatedRequest request);
}
