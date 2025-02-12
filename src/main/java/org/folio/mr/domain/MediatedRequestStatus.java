package org.folio.mr.domain;

import org.folio.mr.domain.dto.MediatedRequest;

public enum MediatedRequestStatus {
  NEW("New"),
  OPEN("Open"),
  CLOSED("Closed");

  private final String value;

  MediatedRequestStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static MediatedRequestStatus fromValue(String value) {
    for (MediatedRequestStatus mediatedRequestStatus : MediatedRequestStatus.values()) {
      if (mediatedRequestStatus.value.equals(value)) {
        return mediatedRequestStatus;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static MediatedRequestStatus from(MediatedRequest.StatusEnum status) {
    return fromValue(status.getValue().split(" - ")[0]);
  }
}
