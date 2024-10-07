package org.folio.mr.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
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

  public static RuntimeException unprocessableEntity(String message) {
    return HttpClientErrorException.create(HttpStatusCode.valueOf(422), message, null, null, null);
  }

  //-----------------------------------------------------------------------------------
  /*
  5xx Errors
   */

  public static RuntimeException unexpectedError(String message, Throwable cause) {
    return new ServerErrorException(message, cause);
  }

}
