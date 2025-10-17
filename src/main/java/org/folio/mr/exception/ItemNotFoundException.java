package org.folio.mr.exception;

import java.util.UUID;

public class ItemNotFoundException extends EntityNotFoundException {

  private static final String ENTITY_NAME = "Item";

  public ItemNotFoundException(UUID id) {
    super(ENTITY_NAME, id);
  }
}
