package org.folio.mr.controller;

import feign.FeignException;
import org.folio.mr.domain.dto.Error;
import org.folio.mr.domain.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class ApiExceptionHandler {

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e) {
    logException(e);
    return buildResponseEntity(e, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
    logException(e);
    return buildResponseEntity(e, HttpStatus.valueOf(e.status()));
  }

  @ExceptionHandler
  public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
    logException(e);
    return buildResponseEntity(e, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  private static void logException(Exception e) {
    log.warn("logException:: handling exception", e);
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(Exception e, HttpStatusCode status) {
    return ResponseEntity.status(status)
      .body(buildErrorResponse(e));
  }

  private static ErrorResponse buildErrorResponse(Exception e) {
    return new ErrorResponse()
      .addErrorsItem(new Error()
        .message(e.getMessage())
        .type(e.getClass().getSimpleName()))
      .totalRecords(1);
  }

}
