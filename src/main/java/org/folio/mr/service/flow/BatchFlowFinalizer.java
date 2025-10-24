package org.folio.mr.service.flow;

import static org.folio.mr.domain.BatchRequestSplitStatus.COMPLETED;
import static org.folio.mr.domain.BatchRequestSplitStatus.FAILED;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchFlowFinalizer extends AbstractBatchRequestStage {

  private final MediatedBatchRequestRepository repository;

  @Override
  @Transactional
  public void execute(BatchContext context) {
    var batchId = context.getBatchRequestId();
    var batchEntity = repository.findById(batchId)
      .orElseThrow(() -> new MediatedBatchRequestNotFoundException(batchId));

    if (atLeastOneSplitEntityIsFailed(context)) {
      log.info("At least one batch split entity is failed, batch with id: {} will be marked as failed", batchId);
      batchEntity.setStatus(BatchRequestStatus.FAILED);
    } else if (allSplitEntitiesAreCompleted(context) || MapUtils.isEmpty(context.getBatchSplitEntitiesById())) {
      log.info("All batch split entities have completed, batch with id: {} will be marked as completed", batchId);
      batchEntity.setStatus(BatchRequestStatus.COMPLETED);
    } else {
      return;
    }

    repository.save(batchEntity);
  }

  private boolean atLeastOneSplitEntityIsFailed(BatchContext context) {
    return context.getBatchSplitEntitiesById().values().stream()
      .map(MediatedBatchRequestSplit::getStatus)
      .anyMatch(FAILED::equals);
  }

  private boolean allSplitEntitiesAreCompleted(BatchContext context) {
    return context.getBatchSplitEntitiesById().values().stream()
      .map(MediatedBatchRequestSplit::getStatus)
      .allMatch(COMPLETED::equals);
  }
}
