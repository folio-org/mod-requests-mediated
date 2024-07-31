package org.folio.mr.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FulfillmentPreference {
  HOLD_SHELF("Hold Shelf"),
  DELIVERY("Delivery");

  private String value;

  FulfillmentPreference(String value) {
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
  public static FulfillmentPreference fromValue(String value) {
    for (FulfillmentPreference b : FulfillmentPreference.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
