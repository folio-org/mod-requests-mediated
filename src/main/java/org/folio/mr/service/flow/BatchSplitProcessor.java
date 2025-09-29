package org.folio.mr.service.flow;

import static org.folio.mr.domain.BatchRequestSplitStatus.FAILED;
import static org.folio.mr.domain.BatchRequestSplitStatus.IN_PROGRESS;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.flow.api.Stage;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchSplitProcessor implements Stage<BatchSplitContext> {

  private final MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @Override
  @Transactional
  public void onStart(BatchSplitContext context) {
    var splitEntity = context.getBatchSplitEntity();

    splitEntity.setStatus(IN_PROGRESS);
    batchRequestSplitRepository.save(splitEntity);
  }

  @Override
  public void execute(BatchSplitContext context) {
    var splitEntity = context.getBatchSplitEntity();
    log.debug("Start processing batch split entity: {}", splitEntity);
    log.info("Create request for item with id: {} at service point with id: {}",
      splitEntity.getItemId(), splitEntity.getPickupServicePointId());
  }

  @Override
  @Transactional
  public void onError(BatchSplitContext context, Exception exception) {
    var splitEntity = context.getBatchSplitEntity();
    var errorMessage = Optional.ofNullable(exception.getCause())
      .map(cause -> exception.getMessage() + ", cause: " + cause.getMessage())
      .orElse(exception.getMessage());

    log.error("Batch split entity with id: {} has failed with error: {}",
      splitEntity.getId(), errorMessage);

    splitEntity.setErrorDetails(errorMessage);
    splitEntity.setStatus(FAILED);
    batchRequestSplitRepository.save(splitEntity);
  }
}
