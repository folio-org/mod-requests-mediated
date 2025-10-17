package org.folio.mr.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.mr.domain.dto.Parameter;
import org.folio.mr.domain.type.ErrorCode;
import org.folio.mr.domain.type.ErrorType;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.exception.ValidationException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import feign.FeignException;


class ApiExceptionHandlerTest {

  private ApiExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ApiExceptionHandler();
  }

  @Test
  void handleEntityNotFoundException_returnsNotFound() {
    var ex = new jakarta.persistence.EntityNotFoundException("Entity not found");
    var response = handler.handleEntityNotFoundException(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("Entity not found", response.getBody().getErrors().getFirst().getMessage());
    assertEquals(ErrorType.NOT_FOUND_ERROR.getValue(), response.getBody().getErrors().getFirst().getType());
  }

  @Test
  void handleFeignException_returnsIntegrationError() {
    var ex = mock(FeignException.class);
    when(ex.status()).thenReturn(502);
    when(ex.getMessage()).thenReturn("Bad Gateway");

    var response = handler.handleFeignException(ex);

    assertEquals(HttpStatus.valueOf(502), response.getStatusCode());
    assertEquals("Bad Gateway", response.getBody().getErrors().getFirst().getMessage());
    assertEquals(ErrorType.INTEGRATION_ERROR.getValue(), response.getBody().getErrors().getFirst().getType());
  }

  @Test
  void handleValidationExceptions_returnsUnprocessableEntity() {
    var errorCode = ErrorCode.DUPLICATE_BATCH_REQUEST_ID;
    var params = List.of(new Parameter().key("field").value("value"));
    var ex = new ValidationException(errorCode, params);

    var response = handler.handleValidationExceptions(ex);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    var error = response.getBody().getErrors().getFirst();
    assertEquals(errorCode.getMessage(), error.getMessage());
    assertEquals(errorCode.getCode(), error.getCode());
    assertEquals(params, error.getParameters());
  }

  @Test
  void conflict_withConstraintViolation_returnsUnprocessableEntity() {
    var cve = mock(ConstraintViolationException.class);
    when(cve.getConstraintName()).thenReturn("pk_batch_request");
    var ex = new DataIntegrityViolationException("Integrity error", cve);

    // Simulate DatabaseConstraintTranslator.translate returning ErrorCode.VALIDATION_ERROR
    var response = handler.conflict(ex);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    var error = response.getBody().getErrors().getFirst();
    assertEquals(ErrorType.VALIDATION_ERROR.getValue(), error.getType());
  }

  @Test
  void conflict_withOtherException_returnsBadRequest() {
    var ex = new InvalidDataAccessApiUsageException("Invalid usage");
    var response = handler.conflict(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var error = response.getBody().getErrors().getFirst();
    assertEquals(ErrorType.VALIDATION_ERROR.getValue(), error.getType());
    assertEquals("Invalid usage", error.getMessage());
  }

  @Test
  void handleUnsupportedOperationException_returnsBadRequest() {
    var ex = new UnsupportedOperationException("Not supported");
    var response = handler.handleUnsupportedOperationException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    var error = response.getBody().getErrors().getFirst();
    assertEquals("Not supported", error.getMessage());
    assertEquals(ErrorType.SERVICE_ERROR.getValue(), error.getType());
  }

  @Test
  void handleNotFoundException_returnsNotFound() {
    var id = UUID.randomUUID();
    var ex = new MediatedBatchRequestNotFoundException(id);
    var response = handler.handleNotFoundException(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    var error = response.getBody().getErrors().getFirst();
    assertEquals("Mediated Batch Request with ID [%s] was not found".formatted(id.toString()), error.getMessage());
    assertEquals(ErrorType.NOT_FOUND_ERROR.getValue(), error.getType());
  }
}
