package org.folio.mr.exception;

import java.util.UUID;

public class MediatedBatchRequestNotFoundException extends EntityNotFoundException {

  private static final String ENTITY_NAME = "Mediated Batch Request";

  public MediatedBatchRequestNotFoundException(UUID id) {
    super(ENTITY_NAME, id);
  }
}
