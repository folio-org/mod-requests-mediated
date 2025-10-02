package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.ValidationException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class BatchFlowInitializerTest {

  @Mock
  private MediatedBatchRequestRepository repository;

  @Mock
  private MediatedBatchRequestSplitRepository splitRepository;

  @Mock
  private BatchContext context;

  @InjectMocks
  private BatchFlowInitializer initializer;

  @Test
  void onStart_shouldSetStatusToInProgress_andSaveEntity() {
    var batchId = UUID.randomUUID();
    var entity = new MediatedBatchRequest();
    entity.setId(batchId);
    entity.setStatus(BatchRequestStatus.PENDING);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.of(entity));

    initializer.onStart(context);

    assertEquals(BatchRequestStatus.IN_PROGRESS, entity.getStatus());
    verify(repository).save(entity);
  }

  @Test
  void onStart_shouldThrowValidationException_whenStatusIsNotPending() {
    var batchId = UUID.randomUUID();
    var entity = new MediatedBatchRequest();
    entity.setId(batchId);
    entity.setStatus(BatchRequestStatus.FAILED);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.of(entity));

    assertThrows(ValidationException.class, () -> initializer.onStart(context));
    verify(repository, never()).save(any());
  }

  @Test
  void onStart_shouldThrowNotFoundException_whenEntityNotFound() {
    var batchId = UUID.randomUUID();
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.empty());

    var ex = assertThrows(RuntimeException.class, () -> initializer.onStart(context));
    assertTrue(ex.getMessage().contains("Mediated Batch Request not found by ID: " + batchId));
  }

  @Test
  void execute_shouldSetBatchSplitEntities_andValidateStatuses() {
    var batchId = UUID.randomUUID();
    var split1 = new MediatedBatchRequestSplit();
    split1.setId(UUID.randomUUID());
    split1.setStatus(BatchRequestSplitStatus.PENDING);

    var split2 = new MediatedBatchRequestSplit();
    split2.setId(UUID.randomUUID());
    split2.setStatus(BatchRequestSplitStatus.PENDING);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(splitRepository.findAllByBatchId(batchId)).thenReturn(List.of(split1, split2));
    when(context.withBatchSplitEntities(any(Map.class))).thenReturn(context);

    assertDoesNotThrow(() -> initializer.execute(context));
    verify(context).withBatchSplitEntities(argThat(map -> map.size() == 2));
  }

  @Test
  void execute_shouldThrowValidationException_whenSplitStatusIsNotPending() {
    var batchId = UUID.randomUUID();
    var split = new MediatedBatchRequestSplit();
    split.setId(UUID.randomUUID());
    split.setStatus(BatchRequestSplitStatus.FAILED);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(splitRepository.findAllByBatchId(batchId)).thenReturn(List.of(split));
    when(context.withBatchSplitEntities(any(Map.class))).thenReturn(context);

    assertThrows(ValidationException.class, () -> initializer.execute(context));
  }

}
