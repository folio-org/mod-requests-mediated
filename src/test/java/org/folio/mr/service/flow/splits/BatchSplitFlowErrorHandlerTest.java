package org.folio.mr.service.flow.splits;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.domain.BatchSplitContext;

@ExtendWith(MockitoExtension.class)
class BatchSplitFlowErrorHandlerTest {

  @InjectMocks private BatchSplitFlowErrorHandler batchSplitFlowErrorHandler;
  @Mock private BatchSplitFlowHelper batchSplitFlowHelper;
  @Mock private BatchSplitContext context;

  @Test
  void execute_positive() {
    batchSplitFlowErrorHandler.execute(context);
    verify(batchSplitFlowHelper).handleExecutionError(context);
  }

  @Test
  void execute_negative_handlerThrowsException() {
    doThrow(RuntimeException.class).when(batchSplitFlowHelper).handleExecutionError(context);
    assertDoesNotThrow(() -> batchSplitFlowErrorHandler.execute(context));
  }
}
