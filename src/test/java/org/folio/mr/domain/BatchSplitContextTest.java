package org.folio.mr.domain;

import static java.util.Collections.emptyMap;
import static org.folio.mr.domain.BatchSplitContext.PARAM_BATCH_SPLIT_REQUEST_ID;
import static org.folio.mr.domain.BatchSplitContext.PARAM_EXECUTION_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.folio.flow.api.StageContext;

class BatchSplitContextTest {

  @Test
  void getBatchSplitRequestId_positive() {
    var splitRequestId = UUID.randomUUID();
    var flowParams = Map.<String, Object>of(PARAM_BATCH_SPLIT_REQUEST_ID, splitRequestId);
    var batchSplitContext = getBatchSplitContext(flowParams);

    var actual = batchSplitContext.getBatchSplitRequestId();

    assertEquals(splitRequestId, actual);
  }

  @Test
  void setExecutionError_positive() {
    var batchSplitContext = getBatchSplitContext(emptyMap());
    var exception = new RuntimeException("Execution failed");

    batchSplitContext.setExecutionError(exception);

    var savedException = batchSplitContext.<Exception>get(PARAM_EXECUTION_ERROR);
    assertEquals(exception, savedException);
  }

  @Test
  void getExecutionError_positive() {
    var batchSplitContext = getBatchSplitContext(emptyMap());
    var exception = new IllegalStateException("Unexpected state");
    batchSplitContext.put(PARAM_EXECUTION_ERROR, exception);

    var actual = batchSplitContext.getExecutionError();

    assertEquals(exception, actual);
  }

  private static BatchSplitContext getBatchSplitContext(Map<String, Object> flowParams) {
    var stageContext = StageContext.of("test", flowParams, emptyMap());
    return new BatchSplitContext(stageContext);
  }
}

