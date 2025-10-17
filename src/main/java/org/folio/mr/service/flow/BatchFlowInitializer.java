package org.folio.mr.service.flow;

import static org.folio.mr.domain.BatchRequestStatus.IN_PROGRESS;
import static org.folio.mr.domain.BatchRequestStatus.PENDING;
import static org.folio.mr.exception.MediatedBatchRequestValidationException.invalidInitialStatus;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.flow.api.Stage;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchFlowInitializer implements Stage<BatchContext> {

  private final MediatedBatchRequestRepository repository;
  private final MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @Override
  public void execute(BatchContext batchContext) {
    validateAndInitBatchExecution(batchContext);

    var batchId = batchContext.getBatchRequestId();
    var batchEntitiesById = batchRequestSplitRepository.findAllByBatchId(batchId).stream()
      .collect(Collectors.toMap(MediatedBatchRequestSplit::getId, Function.identity()));

    batchContext.withBatchSplitEntities(batchEntitiesById);
    validateBatchSplitStatuses(batchEntitiesById.values());
  }

  private void validateAndInitBatchExecution(BatchContext context) {
    // validate batch entity status
    var batchId = context.getBatchRequestId();
    var batchEntity = repository.findById(batchId)
      .orElseThrow(() -> new MediatedBatchRequestNotFoundException(batchId));

    if (batchEntity.getStatus() != PENDING) {
      log.error("Batch entity with id: {} has invalid initial status: {}",
        batchEntity.getId(), batchEntity.getStatus().getValue());
      throw invalidInitialStatus(batchEntity.getStatus().getValue(), batchId);
    }

    if (context.getDeploymentEnvType() == null) {
      throw new IllegalStateException("No Batch Flow Context parameter for deployment environment type was provided");
    }

    // initialize batch execution
    batchEntity.setStatus(IN_PROGRESS);
    repository.save(batchEntity);
  }

  private void validateBatchSplitStatuses(Collection<MediatedBatchRequestSplit> batchRequestSplitList) {
    var entityWithInvalidInitialStatus = batchRequestSplitList.stream()
      .filter(splitEntity -> splitEntity.getStatus() != BatchRequestSplitStatus.PENDING)
      .findFirst();

    if (entityWithInvalidInitialStatus.isPresent()) {
      var splitEntity = entityWithInvalidInitialStatus.get();
      log.error("Batch split entity with id: {} has invalid initial status: {}",
        splitEntity.getId(), splitEntity.getStatus().getValue());

      throw invalidInitialStatus(splitEntity.getStatus().getValue(), splitEntity.getId());
    }
  }
}
