package org.folio.mr.domain;

import java.util.UUID;
import org.folio.flow.api.StageContext;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;


public class BatchSplitContext extends BatchContext {

  public static final String PARAM_BATCH_SPLIT_ENTITY_ID = "mediatedBatchRequestSplitEntityId";

  public BatchSplitContext(StageContext stageContext) {
    super(stageContext);
  }

  public UUID getBatchSplitEntityId() {
    return context.getFlowParameter(PARAM_BATCH_SPLIT_ENTITY_ID);
  }

  public MediatedBatchRequestSplit getBatchSplitEntity() {
    var batchSplitEntities = getBatchSplitEntitiesById();
    var splitEntityId = getBatchSplitEntityId();
    return batchSplitEntities.get(splitEntityId);

  }
}
