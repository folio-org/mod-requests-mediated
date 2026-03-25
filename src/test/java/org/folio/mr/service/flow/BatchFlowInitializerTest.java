package org.folio.mr.service.flow;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.mr.domain.BatchContext;
import org.folio.spring.FolioModuleMetadata;

@ExtendWith(MockitoExtension.class)
class BatchFlowInitializerTest {

  @InjectMocks private BatchFlowInitializer initializer;
  @Mock private BatchFlowHelper batchFlowHelper;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @Mock private BatchContext context;

  @BeforeEach
  void setUp() {
    initializer.setModuleMetadata(folioModuleMetadata);
  }

  @Test
  void execute_positive() {
    when(context.getOkapiHeaders()).thenReturn(Collections.emptyMap());
    initializer.execute(context);
    verify(batchFlowHelper).prepareForFlowExecution(context);
  }

  @Test
  void onError_positive_exceptionWithoutCause() {
    initializer.onError(context, new RuntimeException("Error"));
    verify(context).setBatchRequestFailedMessage("Error");
  }

  @Test
  void onError_positive_exceptionWithCause() {
    var cause = new RuntimeException("Execution error");
    initializer.onError(context, new RuntimeException("Error", cause));
    verify(context).setBatchRequestFailedMessage("Error, cause: Execution error");
  }
}
