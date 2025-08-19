package org.folio.mr.exception;

import java.util.List;

import org.folio.mr.domain.dto.Parameter;
import org.folio.mr.domain.type.ErrorCode;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {

  private final ErrorCode code;
  private final transient List<Parameter> parameters;

  public ValidationException(String message, ErrorCode code, List<Parameter> parameters) {
    super(message);
    this.code = code;
    this.parameters = parameters;
  }

}
