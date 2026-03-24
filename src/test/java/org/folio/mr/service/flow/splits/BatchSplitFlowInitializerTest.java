package org.folio.mr.service.flow.splits;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.domain.BatchSplitContext;

@ExtendWith(MockitoExtension.class)
class BatchSplitFlowInitializerTest {

  @InjectMocks private BatchSplitFlowInitializer batchSplitFlowInitializer;
  @Mock private BatchSplitContext batchSplitContext;
  @Mock private BatchSplitFlowHelper batchSplitFlowHelper;

  @Test
  void execute_positive() {
    batchSplitFlowInitializer.execute(batchSplitContext);
    verify(batchSplitFlowHelper).initializeFlowExecution(batchSplitContext);
  }
}
