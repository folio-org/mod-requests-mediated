package org.folio.mr.service.flow;

import static org.folio.mr.domain.BatchRequestSplitStatus.COMPLETED;
import static org.folio.mr.domain.BatchRequestSplitStatus.FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchFlowFinalizerTest {

  @Mock
  private MediatedBatchRequestRepository repository;

  @Mock
  private BatchContext context;

  @InjectMocks
  private BatchFlowFinalizer finalizer;

  @Test
  void execute_shouldSetStatusToFailed_whenAtLeastOneSplitIsFailed() {
    var batchId = UUID.randomUUID();
    var splitId1 = UUID.randomUUID();
    var splitId2 = UUID.randomUUID();
    var batchEntity = new MediatedBatchRequest();
    batchEntity.setId(batchId);

    var split1 = new MediatedBatchRequestSplit();
    split1.setStatus(COMPLETED);
    var split2 = new MediatedBatchRequestSplit();
    split2.setStatus(FAILED);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.of(batchEntity));
    when(context.getBatchSplitEntitiesById()).thenReturn(Map.of(splitId1, split1, splitId2, split2));

    finalizer.execute(context);

    assertEquals(BatchRequestStatus.FAILED, batchEntity.getStatus());
    verify(repository).save(batchEntity);
  }

  @Test
  void execute_shouldSetStatusToCompleted_whenAllSplitsAreCompleted() {
    var batchId = UUID.randomUUID();
    var splitId1 = UUID.randomUUID();
    var splitId2 = UUID.randomUUID();
    var batchEntity = new MediatedBatchRequest();
    batchEntity.setId(batchId);

    var split1 = new MediatedBatchRequestSplit();
    split1.setStatus(COMPLETED);
    var split2 = new MediatedBatchRequestSplit();
    split2.setStatus(COMPLETED);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.of(batchEntity));
    when(context.getBatchSplitEntitiesById()).thenReturn(Map.of(splitId1, split1, splitId2, split2));

    finalizer.execute(context);

    assertEquals(BatchRequestStatus.COMPLETED, batchEntity.getStatus());
    verify(repository).save(batchEntity);
  }

  @Test
  void execute_shouldNotChangeStatus_whenSplitsAreNotAllCompletedOrFailed() {
    var batchId = UUID.randomUUID();
    var splitId1 = UUID.randomUUID();
    var splitId2 = UUID.randomUUID();
    var batchEntity = new MediatedBatchRequest();
    batchEntity.setId(batchId);
    batchEntity.setStatus(BatchRequestStatus.IN_PROGRESS);

    var split1 = new MediatedBatchRequestSplit();
    split1.setStatus(COMPLETED);
    var split2 = new MediatedBatchRequestSplit();
    split2.setStatus(org.folio.mr.domain.BatchRequestSplitStatus.PENDING);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.of(batchEntity));
    when(context.getBatchSplitEntitiesById()).thenReturn(Map.of(splitId1, split1, splitId2, split2));

    finalizer.execute(context);

    assertEquals(BatchRequestStatus.IN_PROGRESS, batchEntity.getStatus());
    verify(repository, never()).save(any());
  }

  @Test
  void execute_shouldThrowException_whenBatchEntityNotFound() {
    var batchId = UUID.randomUUID();
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.empty());

    var ex = assertThrows(RuntimeException.class, () -> finalizer.execute(context));
    assertTrue(ex.getMessage().contains("Mediated Batch Request not found by ID: " + batchId));
  }
}
