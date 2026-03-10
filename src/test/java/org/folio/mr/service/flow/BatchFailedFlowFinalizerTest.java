package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.mr.domain.BatchContext;

@ExtendWith(MockitoExtension.class)
class BatchFailedFlowFinalizerTest {

  @InjectMocks private BatchFailedFlowFinalizer finalizer;
  @Mock private BatchContext context;
  @Mock private BatchFlowHelper helper;

  @Test
  void execute_positive() {
    finalizer.execute(context);
    verify(helper).handleFailedFlowExecution(context);
  }

  @Test
  void execute_negative_exceptionThrown() {
    doThrow(new RuntimeException("Error")).when(helper).handleFailedFlowExecution(context);
    assertDoesNotThrow(() -> finalizer.execute(context));
    verify(helper).handleFailedFlowExecution(context);
  }
}
