package org.folio.mr.exception;

import java.util.List;

import org.folio.mr.domain.dto.Parameter;
import org.folio.mr.domain.type.ErrorCode;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {

  private final ErrorCode errorCode;
  private final transient List<Parameter> parameters;

  public ValidationException(ErrorCode code, List<Parameter> parameters) {
    super(code.getMessage());
    this.errorCode = code;
    this.parameters = parameters;
  }

  public ValidationException(ErrorCode code, Parameter... parameters) {
    super(code.getMessage());
    this.errorCode = code;
    this.parameters = parameters != null ? List.of(parameters) : null;
  }

}
