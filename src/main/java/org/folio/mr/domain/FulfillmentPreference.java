package org.folio.mr.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FulfillmentPreference {
  HOLD_SHELF("Hold Shelf"),
  DELIVERY("Delivery");

  private final String value;

  FulfillmentPreference(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @JsonCreator
  public static FulfillmentPreference fromValue(String value) {
    for (FulfillmentPreference fulfillmentPreference : FulfillmentPreference.values()) {
      if (fulfillmentPreference.value.equals(value)) {
        return fulfillmentPreference;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
