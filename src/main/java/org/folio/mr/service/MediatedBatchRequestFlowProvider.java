package org.folio.mr.service;

import java.util.UUID;
import org.folio.flow.api.Flow;

public interface MediatedBatchRequestFlowProvider {

  /**
   * Creates mediated batch request specific flow.
   *
   * @param batchId - id of the batch request
   * @return {@link Flow} to process mediated batch request and batch request split records
   */
  Flow createFlow(UUID batchId);
}
