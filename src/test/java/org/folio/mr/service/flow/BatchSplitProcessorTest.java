package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.entity.MediatedBatchRequestSplit;
import org.folio.mr.repository.MediatedBatchRequestSplitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class BatchSplitProcessorTest {

  @Mock
  private MediatedBatchRequestSplitRepository splitRepository;

  @Mock
  private BatchSplitContext context;

  @InjectMocks
  BatchSplitProcessor processor;

  @Test
  void onStart_shouldSetStatusToInProgress_andSaveEntity() {
    var split = new MediatedBatchRequestSplit();
    split.setStatus(BatchRequestSplitStatus.PENDING);
    when(context.getBatchSplitEntity()).thenReturn(split);

    processor.onStart(context);

    assertEquals(BatchRequestSplitStatus.IN_PROGRESS, split.getStatus());
    verify(splitRepository).save(split);
  }

  @Test
  void execute_shouldLogDebugAndInfo() {
    var split = new MediatedBatchRequestSplit();
    split.setItemId(UUID.randomUUID());
    split.setPickupServicePointId(UUID.randomUUID());

    when(context.getBatchSplitEntity()).thenReturn(split);

    assertDoesNotThrow(() -> processor.execute(context));
    // No repository interaction expected
    verifyNoInteractions(splitRepository);
  }

  @Test
  void onError_shouldSetStatusToFailed_andSaveEntity_withErrorDetails() {
    var split = new MediatedBatchRequestSplit();
    split.setId(UUID.randomUUID());
    when(context.getBatchSplitEntity()).thenReturn(split);

    var cause = new RuntimeException("Root cause");
    var ex = new Exception("Processing failed", cause);

    processor.onError(context, ex);

    assertEquals(BatchRequestSplitStatus.FAILED, split.getStatus());
    assertTrue(split.getErrorDetails().contains("Processing failed"));
    assertTrue(split.getErrorDetails().contains("Root cause"));
    verify(splitRepository).save(split);
  }

  @Test
  void onError_shouldSetErrorDetails_whenNoCause() {
    var split = new MediatedBatchRequestSplit();
    split.setId(UUID.randomUUID());
    when(context.getBatchSplitEntity()).thenReturn(split);

    var ex = new Exception("Simple error");

    processor.onError(context, ex);

    assertEquals(BatchRequestSplitStatus.FAILED, split.getStatus());
    assertEquals("Simple error", split.getErrorDetails());
    verify(splitRepository).save(split);
  }
}
