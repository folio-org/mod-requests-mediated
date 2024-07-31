package org.folio.mr.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RequestType {
  HOLD("Hold"),
  RECALL("Recall"),
  PAGE("Page");

  private final String value;

  RequestType(String value) {
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
  public static RequestType fromValue(String value) {
    for (RequestType rt : RequestType.values()) {
      if (rt.value.equals(value)) {
        return rt;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
