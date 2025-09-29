package org.folio.mr.service.flow;

import static org.folio.mr.domain.BatchRequestStatus.FAILED;
import static org.folio.mr.exception.ExceptionFactory.notFound;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.flow.api.Stage;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class BatchFailedFlowFinalizer implements Stage<BatchContext> {

  private final MediatedBatchRequestRepository repository;

  @Override
  @Transactional
  public void execute(BatchContext context) {
    var batchId = context.getBatchRequestId();
    var batchEntity = repository.findById(batchId)
      .orElseThrow(() -> notFound("Mediated Batch Request not found by ID: " + batchId));
    batchEntity.setStatus(FAILED);
    repository.save(batchEntity);
  }
}
