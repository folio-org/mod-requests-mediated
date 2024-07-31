package org.folio.mr.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RequestLevel {
  ITEM("Item"),
  TITLE("Title");

  private final String value;

  RequestLevel(String value) {
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
  public static RequestLevel fromValue(String value) {
    for (RequestLevel rl : RequestLevel.values()) {
      if (rl.value.equals(value)) {
        return rl;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
