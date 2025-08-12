package org.folio.mr.controller;

import java.util.List;

import feign.FeignException;
import org.folio.mr.domain.dto.Error;
import org.folio.mr.domain.dto.ErrorResponse;
import org.folio.mr.domain.dto.Errors;
import org.folio.mr.domain.dto.Parameter;
import org.folio.mr.domain.type.ErrorCode;
import org.folio.mr.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class ApiExceptionHandler {

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e) {
    logException(e);
    return buildResponseEntity(HttpStatus.NOT_FOUND, e);
  }

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
    logException(e);
    return buildResponseEntity(HttpStatus.valueOf(e.status()), e);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<Errors> handleValidationExceptions(ValidationException e) {
    logException(e);
    return buildSingleErrorResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY,
      buildError(e, e.getCode(), e.getParameters()));
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatusCode httpStatusCode,
    Exception e) {

    return ResponseEntity.status(httpStatusCode)
      .body(buildErrorResponse(e));
  }

  private static ResponseEntity<Errors> buildSingleErrorResponseEntity(
    HttpStatusCode httpStatusCode, Error error) {

    return buildResponseEntity(httpStatusCode, new Errors()
      .errors(List.of(error))
      .totalRecords(1));
  }

  private static ErrorResponse buildErrorResponse(Exception e) {
    return new ErrorResponse()
      .addErrorsItem(new Error()
        .message(e.getMessage())
        .type(e.getClass().getSimpleName()))
      .totalRecords(1);
  }

  private static ResponseEntity<Errors> buildResponseEntity(HttpStatusCode httpStatusCode,
    Errors errorResponse) {

    return ResponseEntity.status(httpStatusCode).body(errorResponse);
  }

  private static Error buildError(Exception e, ErrorCode code, List<Parameter> parameters) {
    return new Error(e.getMessage())
      .code(code.getValue())
      .parameters(parameters);
  }

  private static void logException(Exception e) {
    log.warn("logException:: handling exception", e);
  }

}
