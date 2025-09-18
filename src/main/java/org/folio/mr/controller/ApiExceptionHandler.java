package org.folio.mr.controller;

import static org.folio.mr.domain.type.ErrorType.INTEGRATION_ERROR;
import static org.folio.mr.domain.type.ErrorType.NOT_FOUND_ERROR;
import static org.folio.mr.domain.type.ErrorType.VALIDATION_ERROR;
import static org.folio.mr.support.DatabaseConstraintTranslator.translate;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import feign.FeignException;
import org.folio.mr.domain.dto.Error;
import org.folio.mr.domain.dto.ErrorResponse;
import org.folio.mr.domain.dto.Errors;
import org.folio.mr.domain.type.ErrorCode;
import org.folio.mr.domain.type.ErrorType;
import org.folio.mr.exception.ValidationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.log4j.Log4j2;


@RestControllerAdvice
@Log4j2
public class ApiExceptionHandler {

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e) {
    logException(e);
    return buildResponseEntity(HttpStatus.NOT_FOUND, NOT_FOUND_ERROR, e);
  }

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
    logException(e);
    return buildResponseEntity(HttpStatus.valueOf(e.status()), INTEGRATION_ERROR, e);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<Errors> handleValidationExceptions(ValidationException e) {
    logException(e);
    var error = new Error(e.getErrorCode().getMessage())
      .code(e.getErrorCode().getCode())
      .parameters(e.getParameters());
    return buildSingleErrorResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY, error);
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    InvalidDataAccessApiUsageException.class
  })
  public ResponseEntity<ErrorResponse> conflict(Exception e) {
    var cause = e.getCause();
    if (cause instanceof ConstraintViolationException cve) {
      var errorCode = translate(cve);
      return buildResponseEntity(errorCode, VALIDATION_ERROR, UNPROCESSABLE_ENTITY);
    }
    return buildResponseEntity(BAD_REQUEST, VALIDATION_ERROR, e);
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatusCode httpStatusCode, ErrorType type,
    Exception e) {

    return ResponseEntity.status(httpStatusCode)
      .body(buildErrorResponse(e, type));
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(ErrorCode errorCode, ErrorType type, HttpStatusCode status) {
    var errors = new ErrorResponse()
      .errors(List.of(new Error(errorCode.getMessage())
        .type(type.getValue())
        .code(errorCode.getCode())))
      .totalRecords(1);
    return ResponseEntity.status(status).body(errors);
  }

  private static ResponseEntity<Errors> buildSingleErrorResponseEntity(
    HttpStatusCode httpStatusCode, Error error) {

    return buildResponseEntity(httpStatusCode, new Errors()
      .errors(List.of(error))
      .totalRecords(1));
  }

  private static ErrorResponse buildErrorResponse(Exception e, ErrorType type) {
    return new ErrorResponse()
      .addErrorsItem(new Error()
        .message(e.getMessage())
        .type(type.getValue()))
      .totalRecords(1);
  }

  private static ResponseEntity<Errors> buildResponseEntity(HttpStatusCode httpStatusCode, Errors errorResponse) {
    return ResponseEntity.status(httpStatusCode).body(errorResponse);
  }

  private static void logException(Exception e) {
    log.warn("logException:: handling exception", e);
  }

}
