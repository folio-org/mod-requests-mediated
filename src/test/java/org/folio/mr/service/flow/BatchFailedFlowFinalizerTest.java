package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchRequestStatus;
import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.entity.MediatedBatchRequest;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.repository.MediatedBatchRequestRepository;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class BatchFailedFlowFinalizerTest {

  @Mock
  private MediatedBatchRequestRepository repository;
  @Mock
  private MediatedBatchRequestSplitRepository batchRequestSplitRepository;

  @Mock
  private BatchContext context;

  @InjectMocks
  private BatchFailedFlowFinalizer finalizer;

  @Test
  void execute_positive_shouldSetStatusToFailedAndSaveEntity() {
    var batchId = UUID.randomUUID();
    var entity = new MediatedBatchRequest();
    entity.setId(batchId);
    var split1 = new MediatedBatchRequestSplit();
    var split2 = MediatedBatchRequestSplit.builder()
      .status(BatchRequestSplitStatus.COMPLETED)
      .confirmedRequestId(UUID.randomUUID())
      .build();

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getBatchRequestFailedMessage()).thenReturn("batch request failed");
    when(repository.findById(batchId)).thenReturn(Optional.of(entity));
    when(batchRequestSplitRepository.findAllByBatchId(batchId)).thenReturn(List.of(split1, split2));
    var captor = ArgumentCaptor.forClass(List.class);

    finalizer.execute(context);

    assertEquals(BatchRequestStatus.FAILED, entity.getStatus());
    verify(repository).save(entity);
    verify(batchRequestSplitRepository).saveAll(captor.capture());
    var savedSplits = captor.getValue();
    assertEquals(2, savedSplits.size());
    assertEquals(BatchRequestSplitStatus.FAILED, ((MediatedBatchRequestSplit) savedSplits.getFirst()).getStatus());
    assertEquals("batch request failed", ((MediatedBatchRequestSplit) savedSplits.getFirst()).getErrorDetails());
  }

  @Test
  void execute_positive_shouldSetStatusToFailedForBatchEntityOnly() {
    var batchId = UUID.randomUUID();
    var entity = new MediatedBatchRequest();
    entity.setId(batchId);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getBatchRequestFailedMessage()).thenReturn("");
    when(repository.findById(batchId)).thenReturn(Optional.of(entity));

    finalizer.execute(context);

    assertEquals(BatchRequestStatus.FAILED, entity.getStatus());
    verify(repository).save(entity);
    verifyNoInteractions(batchRequestSplitRepository);
  }

  @Test
  void execute_negative_shouldThrowExceptionWhenEntityNotFound() {
    var batchId = UUID.randomUUID();
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(repository.findById(batchId)).thenReturn(Optional.empty());

    var ex = assertThrows(MediatedBatchRequestNotFoundException.class, () -> finalizer.execute(context));
    assertTrue(ex.getMessage().contains("Mediated Batch Request with ID [%s] was not found".formatted(batchId)));
    verify(repository, never()).save(any());
  }
}
