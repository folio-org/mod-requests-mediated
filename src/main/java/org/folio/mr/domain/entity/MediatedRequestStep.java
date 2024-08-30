package org.folio.mr.domain.entity;

import org.folio.mr.domain.dto.MediatedRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MediatedRequestStep {
  AWAITING_CONFIRMATION("Awaiting confirmation"),
  NOT_YET_FILLED("Not yet filled"),
  IN_TRANSIT_FOR_APPROVAL("In transit for approval"),
  ITEM_ARRIVED("Item arrived"),
  IN_TRANSIT_TO_BE_CHECKED_OUT("In transit to be checked out"),
  AWAITING_PICKUP("Awaiting pickup"),
  CANCELLED("Cancelled"),
  DECLINED("Declined"),
  FILLED("Filled");

  private final String value;

  public static MediatedRequestStep fromValue(String value) {
    for (MediatedRequestStep step : values()) {
      if (step.value.equals(value)) {
        return step;
      }
    }
    throw new IllegalArgumentException("Unexpected value: '" + value + "'");
  }

  public static MediatedRequestStep from(MediatedRequest.StatusEnum status) {
    return fromValue(status.getValue().split(" - ")[1]);
  }

}