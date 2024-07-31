package org.folio.mr.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MediatedRequestStatus {
  NEW("New"),
  OPEN("Open"),
  CLOSED("Closed");

  private String value;

  MediatedRequestStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static MediatedRequestStatus fromValue(String value) {
    for (MediatedRequestStatus b : MediatedRequestStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
