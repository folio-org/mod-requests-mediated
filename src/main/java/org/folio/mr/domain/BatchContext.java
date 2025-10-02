package org.folio.mr.domain;

import java.util.Map;
import java.util.UUID;
import org.folio.flow.api.AbstractStageContextWrapper;
import org.folio.flow.api.StageContext;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;


public class BatchContext extends AbstractStageContextWrapper {
   public static final String PARAM_BATCH_ID = "mediatedBatchRequestId";
  public static final String PARAM_BATCH_SPLIT_ENTITIES = "mediatedBatchSplitEntities";

  public BatchContext(StageContext stageContext) {
    super(stageContext);
  }

  public UUID getBatchRequestId() {
    return context.getFlowParameter(PARAM_BATCH_ID);
  }

  public BatchContext withBatchSplitEntities(Map<UUID, MediatedBatchRequestSplit> batchSplitEntities) {
    context.put(PARAM_BATCH_SPLIT_ENTITIES, batchSplitEntities);
    return this;
  }

  public Map<UUID, MediatedBatchRequestSplit> getBatchSplitEntitiesById() {
    return context.get(PARAM_BATCH_SPLIT_ENTITIES);
  }
}
