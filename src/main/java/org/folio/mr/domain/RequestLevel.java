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

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static RequestLevel fromValue(String value) {
    for (RequestLevel requestLevel : RequestLevel.values()) {
      if (requestLevel.value.equals(value)) {
        return requestLevel;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
