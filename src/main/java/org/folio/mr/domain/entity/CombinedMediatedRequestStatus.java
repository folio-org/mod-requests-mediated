package org.folio.mr.domain.entity;

import static org.folio.mr.domain.MediatedRequestStatus.CLOSED;
import static org.folio.mr.domain.MediatedRequestStatus.NEW;
import static org.folio.mr.domain.MediatedRequestStatus.OPEN;
import static org.folio.mr.domain.entity.MediatedRequestStep.AWAITING_CONFIRMATION;
import static org.folio.mr.domain.entity.MediatedRequestStep.AWAITING_PICKUP;
import static org.folio.mr.domain.entity.MediatedRequestStep.CANCELLED;
import static org.folio.mr.domain.entity.MediatedRequestStep.DECLINED;
import static org.folio.mr.domain.entity.MediatedRequestStep.FILLED;
import static org.folio.mr.domain.entity.MediatedRequestStep.IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.entity.MediatedRequestStep.IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.entity.MediatedRequestStep.ITEM_ARRIVED;
import static org.folio.mr.domain.entity.MediatedRequestStep.NOT_YET_FILLED;

import org.folio.mr.domain.MediatedRequestStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CombinedMediatedRequestStatus {
  NEW_AWAITING_CONFIRMATION(NEW, AWAITING_CONFIRMATION),
  OPEN_NOT_YET_FILLED(OPEN, NOT_YET_FILLED),
  OPEN_IN_TRANSIT_FOR_APPROVAL(OPEN, IN_TRANSIT_FOR_APPROVAL),
  OPEN_ITEM_ARRIVED(OPEN, ITEM_ARRIVED),
  OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT(OPEN, IN_TRANSIT_TO_BE_CHECKED_OUT),
  OPEN_AWAITING_PICKUP(OPEN, AWAITING_PICKUP),
  CLOSED_CANCELLED(CLOSED, CANCELLED),
  CLOSED_DECLINED(CLOSED, DECLINED),
  CLOSED_FILLED(CLOSED, FILLED);

  private final MediatedRequestStatus mediatedRequestStatus;
  private final MediatedRequestStep mediatedRequestStep;

  public String getValue() {
    return mediatedRequestStatus.getValue() + " - " + mediatedRequestStep.getValue();
  }
}
