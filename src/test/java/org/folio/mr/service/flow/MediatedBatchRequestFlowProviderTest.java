package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.service.TenantSupportService;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class MediatedBatchRequestFlowProviderTest {

  @Mock
  private BatchFlowInitializer batchFlowInitializer;
  @Mock
  private BatchSplitProcessor batchSplitProcessor;
  @Mock
  private BatchFailedFlowFinalizer failedFlowFinalizer;
  @Mock
  private BatchFlowFinalizer flowFinalizer;
  @Mock
  private MediatedBatchRequestFlowProviderImpl provider;
  @Mock
  private TenantSupportService tenantSupportService;
  @Mock
  private FolioExecutionContext executionContext;

  @BeforeEach
  void setUp() {
    provider = new MediatedBatchRequestFlowProviderImpl(
      batchFlowInitializer, batchSplitProcessor, failedFlowFinalizer, flowFinalizer, tenantSupportService, executionContext
    );
  }

  @Test
  void createFlow_positive_shouldBuildFlowWithCorrectStagesAndParameters() {
    var batchId = UUID.randomUUID();
    when(executionContext.getTenantId()).thenReturn("tenantId");
    when(tenantSupportService.isSecureTenant(any(String.class))).thenReturn(false);
    when(tenantSupportService.isCentralTenant(any(String.class))).thenReturn(true);

    var flow = provider.createFlow(batchId);

    assertNotNull(flow);
    assertEquals("BatchRequestFlow/" + batchId, flow.getId());
    assertEquals(batchId, flow.getFlowParameters().get(BatchContext.PARAM_BATCH_ID));
    assertEquals(EnvironmentType.ECS, flow.getFlowParameters().get(BatchContext.PARAM_DEPLOYMENT_ENV_TYPE));
    assertEquals(3, flow.getStages().size());
    assertEquals("BatchSplitEntitiesDynamicStage", flow.getStages().get(1).getStageId());
    assertEquals("DynamicStage", flow.getStages().get(1).getStageType());
  }

  @Test
  void createFlow_negative_shouldThrowNotSupportedErrorForSecureTenant() {
    var batchId = UUID.randomUUID();
    when(executionContext.getTenantId()).thenReturn("secureTenant");
    when(tenantSupportService.isSecureTenant(any(String.class))).thenReturn(true);

    assertThrows(UnsupportedOperationException.class, () -> provider.createFlow(batchId),
      "Multi-Item Request is not supported for secure tenant: secureTenant");
  }
}
