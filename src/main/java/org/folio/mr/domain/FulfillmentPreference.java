package org.folio.mr.domain;

public enum FulfillmentPreference {
  HOLD_SHELF("Hold Shelf"),
  DELIVERY("Delivery");

  private final String value;

  FulfillmentPreference(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static FulfillmentPreference fromValue(String value) {
    for (FulfillmentPreference fulfillmentPreference : FulfillmentPreference.values()) {
      if (fulfillmentPreference.value.equals(value)) {
        return fulfillmentPreference;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
