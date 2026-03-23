package org.folio.mr.service.flow;

import static org.folio.mr.service.flow.EnvironmentType.ECS;
import static org.folio.mr.service.flow.EnvironmentType.SINGLE_TENANT;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.folio.flow.api.DynamicStage;
import org.folio.flow.api.Flow;
import org.folio.flow.api.ParallelStage;
import org.folio.flow.api.StageContext;
import org.folio.flow.model.FlowExecutionStrategy;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.service.MediatedBatchRequestFlowProvider;
import org.folio.mr.service.TenantSupportService;
import org.folio.mr.service.flow.splits.BatchSplitFlowErrorHandler;
import org.folio.mr.service.flow.splits.BatchSplitFlowInitializer;
import org.folio.mr.service.flow.splits.BatchSplitProcessor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediatedBatchRequestFlowProviderImpl implements MediatedBatchRequestFlowProvider {

  private final BatchFlowInitializer batchFlowInitializer;
  private final BatchFailedFlowFinalizer failedFlowFinalizer;
  private final BatchFlowFinalizer flowFinalizer;
  private final TenantSupportService tenantSupportService;
  private final FolioExecutionContext executionContext;

  private final BatchSplitProcessor batchSplitProcessor;
  private final BatchSplitFlowInitializer batchSplitInitializer;
  private final BatchSplitFlowErrorHandler batchSplitErrorHandler;

  @Override
  public Flow createFlow(UUID batchId) {
    var batchFlowId = "BatchRequestFlow/" + batchId;
    var envType = getEnvironmentType();

    log.info("Creating batch request processing flow for batchId: {} and {} environment", batchId, envType.name());

    return Flow.builder()
      .id(batchFlowId)
      .stage(batchFlowInitializer)
      .stage(DynamicStage.of("BatchSplitEntitiesDynamicStage", this::batchSplitEntitiesFlowProvider))
      .stage(flowFinalizer)
      .onFlowError(failedFlowFinalizer)
      .executionStrategy(FlowExecutionStrategy.IGNORE_ON_ERROR)
      .flowParameter(BatchContext.PARAM_BATCH_ID, batchId)
      .flowParameter(BatchContext.PARAM_OKAPI_HEADERS, executionContext.getOkapiHeaders())
      .flowParameter(BatchContext.PARAM_DEPLOYMENT_ENV_TYPE, envType)
      .build();
  }

  private Flow batchSplitEntitiesFlowProvider(StageContext context) {
    var batchContext = new BatchContext(context);

    var stages = batchContext.getBatchSplitEntityIds().stream()
      .map(splitEntityId -> Flow.builder()
        .id("BatchSplitEntityFlow/" + splitEntityId)
        .stage(batchSplitInitializer)
        .stage(batchSplitProcessor)
        .onFlowError(batchSplitErrorHandler)
        .executionStrategy(FlowExecutionStrategy.IGNORE_ON_ERROR)
        .flowParameter(BatchSplitContext.PARAM_BATCH_SPLIT_REQUEST_ID, splitEntityId)
        .build())
      .toList();

    return Flow.builder()
      .id("BatchSplitEntitiesCombinedFlow")
      .stage(ParallelStage.of("BatchSplitEntitiesParallelStage", stages))
      .build();
  }

  private EnvironmentType getEnvironmentType() {
    var tenantId = executionContext.getTenantId();
    if (tenantSupportService.isSecureTenant(tenantId)) {
      return EnvironmentType.SECURE_TENANT;
    }

    if (tenantSupportService.isCentralTenant(tenantId)) {
      return ECS;
    }

    return SINGLE_TENANT;
  }
}
