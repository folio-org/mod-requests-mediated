package org.folio.mr.service.flow;

import static org.folio.mr.domain.BatchRequestStatus.FAILED;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchFailedFlowFinalizer extends AbstractBatchRequestStage {

  private final MediatedBatchRequestRepository repository;
  private final MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @Override
  @Transactional
  public void execute(BatchContext context) {
    var batchId = context.getBatchRequestId();
    var batchEntity = repository.findById(batchId)
      .orElseThrow(() -> new MediatedBatchRequestNotFoundException(batchId));
    batchEntity.setStatus(FAILED);
    repository.save(batchEntity);

    var errorMessage = context.getBatchRequestFailedMessage();
    var splitEntities = batchRequestSplitRepository.findAllByBatchId(batchId);
    splitEntities.forEach(requestSplit -> {
        requestSplit.setStatus(BatchRequestSplitStatus.FAILED);
        requestSplit.setErrorDetails(errorMessage);
    });
    batchRequestSplitRepository.saveAll(splitEntities);
  }
}
