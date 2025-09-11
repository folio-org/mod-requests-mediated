package org.folio.mr.domain;

import lombok.Getter;

@Getter
public enum BatchRequestSplitStatus {
  PENDING("Pending"),
  IN_PROGRESS("In progress"),
  COMPLETED("Completed"),
  FAILED("Failed");

  private final String value;

  @Override
  public String toString() {
    return value;
  }

  BatchRequestSplitStatus(String value) {
    this.value = value;
  }

  public static BatchRequestSplitStatus fromValue(String value) {
    for (BatchRequestSplitStatus status : BatchRequestSplitStatus.values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
