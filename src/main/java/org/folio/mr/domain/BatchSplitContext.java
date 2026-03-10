package org.folio.mr.domain;

import java.util.UUID;
import org.folio.flow.api.StageContext;

public class BatchSplitContext extends BatchContext {

  public static final String PARAM_BATCH_SPLIT_REQUEST_ID = "mediatedBatchRequestSplitRequestId";
  public static final String PARAM_EXECUTION_ERROR = "executionError";

  public BatchSplitContext(StageContext stageContext) {
    super(stageContext);
  }

  public UUID getBatchSplitRequestId() {
    return context.getFlowParameter(PARAM_BATCH_SPLIT_REQUEST_ID);
  }

  public void setExecutionError(Exception exception) {
    context.put(PARAM_EXECUTION_ERROR, exception);
  }

  public Exception getExecutionError() {
    return context.get(PARAM_EXECUTION_ERROR);
  }
}
