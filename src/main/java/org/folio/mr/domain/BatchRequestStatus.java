package org.folio.mr.domain;

import lombok.Getter;

@Getter
public enum BatchRequestStatus {
  PENDING("Pending"),
  IN_PROGRESS("In progress"),
  COMPLETED("Completed"),
  FAILED("Failed");

  private final String value;

  @Override
  public String toString() {
    return value;
  }

  BatchRequestStatus(String value) {
    this.value = value;
  }

  public static BatchRequestStatus fromValue(String value) {
    for (BatchRequestStatus status : BatchRequestStatus.values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
