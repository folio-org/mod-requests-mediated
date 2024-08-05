package org.folio.mr.domain;

public enum RequestType {
  HOLD("Hold"),
  RECALL("Recall"),
  PAGE("Page");

  private final String value;

  RequestType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static RequestType fromValue(String value) {
    for (RequestType requestType : RequestType.values()) {
      if (requestType.value.equals(value)) {
        return requestType;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
