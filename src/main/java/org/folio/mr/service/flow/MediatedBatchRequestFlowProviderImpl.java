package org.folio.mr.service.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.flow.api.DynamicStage;
import org.folio.flow.api.Flow;
import org.folio.flow.api.ParallelStage;
import org.folio.flow.api.Stage;
import org.folio.flow.api.StageContext;
import org.folio.flow.model.FlowExecutionStrategy;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.service.MediatedBatchRequestFlowProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediatedBatchRequestFlowProviderImpl implements MediatedBatchRequestFlowProvider {

  private final BatchFlowInitializer batchFlowInitializer;
  private final BatchSplitProcessor batchSplitProcessor;
  private final BatchFailedFlowFinalizer failedFlowFinalizer;
  private final BatchFlowFinalizer flowFinalizer;

  @Override
  public Flow createFlow(UUID batchId) {
    var batchFlowId = "BatchRequestFlow/" + batchId;

    return Flow.builder()
      .id(batchFlowId)
      .stage(batchFlowInitializer)
      .stage(DynamicStage.of("BatchSplitEntitiesDynamicStage", this::batchSplitEntitiesFlowProvider))
      .stage(flowFinalizer)
      .onFlowError(failedFlowFinalizer)
      .executionStrategy(FlowExecutionStrategy.IGNORE_ON_ERROR)
      .flowParameter(BatchContext.PARAM_BATCH_ID, batchId)
      .build();
  }

  private Flow batchSplitEntitiesFlowProvider(StageContext context) {
    var batchContext = new BatchContext(context);
    var flowBuilder = Flow.builder().id("BatchSplitEntitiesCombinedFlow");
    List<Stage<StageContext>> stages = new ArrayList<>();

    batchContext.getBatchSplitEntitiesById().values().forEach(splitEntity -> {
      var splitFlowId = "BatchSplitEntityFlow/" + splitEntity.getId();
      var splitFlow = Flow.builder()
        .id(splitFlowId)
        .stage(batchSplitProcessor)
        .executionStrategy(FlowExecutionStrategy.IGNORE_ON_ERROR)
        .flowParameter(BatchSplitContext.PARAM_BATCH_SPLIT_ENTITY_ID, splitEntity.getId())
        .build();
      stages.add(splitFlow);
    });

    var combinedFlow = ParallelStage.of("BatchSplitEntitiesParallelStage", stages);
    return flowBuilder.stage(combinedFlow).build();
  }
}
