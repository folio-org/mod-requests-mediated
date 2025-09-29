package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class BatchFailedFlowFinalizerTest {

  @Mock
  private MediatedBatchRequestRepository repository;

  @Mock
  private BatchContext context;

  @InjectMocks
  private BatchFailedFlowFinalizer finalizer;

  @Test
  void execute_shouldSetStatusToFailed_andSaveEntity() {
    var batchId = UUID.randomUUID();
    var entity = new MediatedBatchRequest();
    entity.setId(batchId);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.of(entity));

    finalizer.execute(context);

    assertEquals(BatchRequestStatus.FAILED, entity.getStatus());
    verify(repository).save(entity);
  }

  @Test
  void execute_shouldThrowException_whenEntityNotFound() {
    var batchId = UUID.randomUUID();
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.empty());

    var ex = assertThrows(RuntimeException.class, () -> finalizer.execute(context));
    assertTrue(ex.getMessage().contains("Mediated Batch Request not found by ID: " + batchId));
    verify(repository, never()).save(any());
  }
}
