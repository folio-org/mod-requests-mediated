package org.folio.mr.exception;

public class ItemNotFoundException extends EntityNotFoundException {

  private static final String ENTITY_NAME = "Item";

  public ItemNotFoundException(String id) {
    super(ENTITY_NAME, id);
  }
}
