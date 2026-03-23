package org.folio.mr.service.flow.splits;

import static org.folio.mr.domain.BatchRequestSplitStatus.IN_PROGRESS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.service.MediatedBatchRequestSplitService;

@ExtendWith(MockitoExtension.class)
class BatchSplitFlowInitializerTest {

  @InjectMocks private BatchSplitFlowInitializer batchSplitFlowInitializer;
  @Mock private BatchSplitContext batchSplitContext;
  @Mock private MediatedBatchRequestSplitService batchRequestSplitService;

  @Test
  void execute_positive() {
    var batchSplitId = UUID.randomUUID();
    when(batchSplitContext.getBatchSplitRequestId()).thenReturn(batchSplitId);
    batchSplitFlowInitializer.execute(batchSplitContext);
    verify(batchRequestSplitService).updateStatusById(batchSplitId, IN_PROGRESS);
  }
}
