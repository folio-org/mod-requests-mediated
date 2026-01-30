package org.folio.mr.exception;

public class HoldingNotFoundException extends EntityNotFoundException {

  private static final String ENTITY_NAME = "Holding";

  public HoldingNotFoundException(String id) {
    super(ENTITY_NAME, id);
  }
}
