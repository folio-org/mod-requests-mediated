package org.folio.mr.exception;

public abstract class EntityNotFoundException extends RuntimeException {

  private static final String NOT_FOUND_MSG_TEMPLATE = "%s with ID [%s] was not found";

  protected EntityNotFoundException(String entityName, Object id) {
    super(String.format(NOT_FOUND_MSG_TEMPLATE, entityName, id));
  }
}
