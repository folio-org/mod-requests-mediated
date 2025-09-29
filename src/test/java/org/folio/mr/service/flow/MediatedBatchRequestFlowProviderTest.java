package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.folio.mr.domain.BatchContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class MediatedBatchRequestFlowProviderTest {

  private BatchFlowInitializer batchFlowInitializer;
  private BatchSplitProcessor batchSplitProcessor;
  private BatchFailedFlowFinalizer failedFlowFinalizer;
  private BatchFlowFinalizer flowFinalizer;
  private MediatedBatchRequestFlowProviderImpl provider;

  @BeforeEach
  void setUp() {
    batchFlowInitializer = mock(BatchFlowInitializer.class);
    batchSplitProcessor = mock(BatchSplitProcessor.class);
    failedFlowFinalizer = mock(BatchFailedFlowFinalizer.class);
    flowFinalizer = mock(BatchFlowFinalizer.class);
    provider = new MediatedBatchRequestFlowProviderImpl(
      batchFlowInitializer, batchSplitProcessor, failedFlowFinalizer, flowFinalizer
    );
  }

  @Test
  void createFlow_shouldBuildFlowWithCorrectStagesAndParameters() {
    var batchId = UUID.randomUUID();
    var flow = provider.createFlow(batchId);

    assertNotNull(flow);
    assertEquals("BatchRequestFlow/" + batchId, flow.getId());
    assertEquals(batchId, flow.getFlowParameters().get(BatchContext.PARAM_BATCH_ID));
    assertEquals(3, flow.getStages().size());
    assertEquals("BatchSplitEntitiesDynamicStage", flow.getStages().get(1).getStageId());
    assertEquals("DynamicStage", flow.getStages().get(1).getStageType());
  }
}
