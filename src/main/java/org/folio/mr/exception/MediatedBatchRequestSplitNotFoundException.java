package org.folio.mr.exception;

import java.util.UUID;

public class MediatedBatchRequestSplitNotFoundException extends EntityNotFoundException {

  private static final String ENTITY_NAME = "Mediated Batch Request Split";

  public MediatedBatchRequestSplitNotFoundException(UUID id) {
    super(ENTITY_NAME, id);
  }
}
