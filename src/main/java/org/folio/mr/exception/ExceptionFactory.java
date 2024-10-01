package org.folio.mr.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.experimental.UtilityClass;

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

}
