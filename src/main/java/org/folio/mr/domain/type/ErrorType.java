package org.folio.mr.domain.type;

import lombok.Getter;

@Getter
public enum ErrorType {

  VALIDATION_ERROR("validation"),
  NOT_FOUND_ERROR("not-found"),
  INTEGRATION_ERROR("integration-error"),
  SERVICE_ERROR("service_error"),
  UNKNOWN_ERROR("unknown");

  private final String value;

  ErrorType(String value) {
    this.value = value;
  }
}
