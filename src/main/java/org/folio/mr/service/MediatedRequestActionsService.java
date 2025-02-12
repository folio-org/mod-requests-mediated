package org.folio.mr.service;

import java.util.UUID;

import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.entity.MediatedRequestWorkflowLog;

public interface MediatedRequestActionsService {
  void confirm(UUID mediatedRequestId);
  MediatedRequest confirmItemArrival(String itemBarcode);
  MediatedRequest sendItemInTransit(String itemBarcode);
  void decline(UUID mediatedRequestId);
  void changeStatusToInTransitForApproval(MediatedRequestEntity request);
  void changeStatusToAwaitingPickup(MediatedRequestEntity request);
  void changeStatusToClosedFilled(MediatedRequestEntity request);
  void changeStatusToClosedCanceled(MediatedRequestEntity mediatedRequest, Request confirmedRequest);
  MediatedRequestWorkflowLog saveMediatedRequestWorkflowLog(MediatedRequest request);
}
