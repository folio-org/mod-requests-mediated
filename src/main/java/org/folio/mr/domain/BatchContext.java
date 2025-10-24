package org.folio.mr.domain;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.flow.api.AbstractStageContextWrapper;
import org.folio.flow.api.StageContext;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.service.flow.EnvironmentType;


public class BatchContext extends AbstractStageContextWrapper {
  public static final String PARAM_BATCH_ID = "mediatedBatchRequestId";
  public static final String PARAM_BATCH_SPLIT_ENTITIES = "mediatedBatchSplitEntities";
  public static final String PARAM_BATCH_PROCESS_FAILED_MESSAGE = "mediatedBatchProcessFailedMessages";
  public static final String PARAM_DEPLOYMENT_ENV_TYPE = "deploymentEnvType";

  public BatchContext(StageContext stageContext) {
    super(stageContext);
  }

  public UUID getBatchRequestId() {
    return context.getFlowParameter(PARAM_BATCH_ID);
  }

  public EnvironmentType getDeploymentEnvType() {
    return context.getFlowParameter(PARAM_DEPLOYMENT_ENV_TYPE);
  }

  public BatchContext withBatchSplitEntities(Map<UUID, MediatedBatchRequestSplit> batchSplitEntities) {
    context.put(PARAM_BATCH_SPLIT_ENTITIES, batchSplitEntities);
    return this;
  }

  public Map<UUID, MediatedBatchRequestSplit> getBatchSplitEntitiesById() {
    return context.get(PARAM_BATCH_SPLIT_ENTITIES);
  }

  public void setBatchRequestFailedMessage(String failureMessage) {
    context.put(PARAM_BATCH_PROCESS_FAILED_MESSAGE, failureMessage);
  }

  public String getBatchRequestFailedMessage() {
    return Optional.ofNullable(context.get(PARAM_BATCH_PROCESS_FAILED_MESSAGE))
      .map(Object::toString)
      .orElse("");
  }
}
