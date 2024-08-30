package org.folio.mr.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.folio.mr.domain.dto.Error;
import org.folio.mr.domain.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
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
    return buildResponseEntity(e, NOT_FOUND);
  }

  private static void logException(Exception e) {
    log.warn("logException:: handling exeption", e);
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(Exception e, HttpStatus status) {
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
