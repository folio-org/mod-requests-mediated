package org.folio.mr.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.experimental.UtilityClass;
import org.springframework.web.server.ServerErrorException;

@UtilityClass
public class ExceptionFactory {

  //-----------------------------------------------------------------------------------
  /*
  4xx Errors
   */

  public static RuntimeException notFound(String message) {
    return new EntityNotFoundException(message);
  }

  public static RuntimeException badRequest(String message) {
    return new ValidationException(message);
  }

  //-----------------------------------------------------------------------------------
  /*
  5xx Errors
   */

  public static RuntimeException unexpectedError(String message, Throwable cause) {
    return new ServerErrorException(message, cause);
  }

}
