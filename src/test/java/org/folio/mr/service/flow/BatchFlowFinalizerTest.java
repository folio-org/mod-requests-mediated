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
class BatchFlowFinalizerTest {

  @InjectMocks private BatchFlowFinalizer finalizer;
  @Mock private BatchContext context;
  @Mock private BatchFlowHelper batchFlowHelper;
  @Mock private FolioModuleMetadata folioModuleMetadata;

  @BeforeEach
  void setUp() {
    finalizer.setModuleMetadata(folioModuleMetadata);
  }

  @Test
  void execute_positive() {
    when(context.getOkapiHeaders()).thenReturn(Collections.emptyMap());
    finalizer.execute(context);
    verify(batchFlowHelper).finalizeFlowExecution(context);
  }
}
