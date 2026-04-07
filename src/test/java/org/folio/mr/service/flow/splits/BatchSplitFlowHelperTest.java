package org.folio.mr.service.flow.splits;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.domain.BatchRequestSplitStatus;
import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto.MediatedRequestStatusEnum;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;

@ExtendWith(MockitoExtension.class)
class BatchSplitFlowHelperTest {

  @InjectMocks private BatchSplitFlowHelper helper;
  @Mock private BatchSplitContext context;
  @Mock private MediatedBatchRequestDetailDto splitRecord;
  @Mock private MediatedBatchRequestsService batchRequestsService;
  @Mock private MediatedBatchRequestSplitService batchRequestSplitService;

  @Test
  void handleExecutionError_positive_errorWithCause() {
    var splitRequestId = UUID.randomUUID();
    var exception = new RuntimeException("ERROR", new RuntimeException("Error Cause"));
    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(context.getExecutionError()).thenReturn(exception);
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(splitRecord);

    helper.handleExecutionError(context);

    verify(splitRecord).setErrorDetails("ERROR, cause: Error Cause");
    verify(splitRecord).setMediatedRequestStatus(MediatedRequestStatusEnum.FAILED);
    verify(batchRequestSplitService).update(splitRequestId, splitRecord);
  }

  @Test
  void handleExecutionError_positive_errorWithoutCause() {
    var splitRequestId = UUID.randomUUID();
    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(context.getExecutionError()).thenReturn(new RuntimeException("Execution Test Error"));
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(splitRecord);

    helper.handleExecutionError(context);

    verify(splitRecord).setErrorDetails("Execution Test Error");
    verify(splitRecord).setMediatedRequestStatus(MediatedRequestStatusEnum.FAILED);
    verify(batchRequestSplitService).update(splitRequestId, splitRecord);
  }

  @Test
  void handleExecutionError_positive_errorWithoutMessage() {
    var splitRequestId = UUID.randomUUID();
    var itemId = UUID.randomUUID().toString();
    when(context.getBatchSplitRequestId()).thenReturn(splitRequestId);
    when(context.getExecutionError()).thenReturn(new NullPointerException());
    when(splitRecord.getItemId()).thenReturn(itemId);
    when(batchRequestSplitService.getById(splitRequestId)).thenReturn(splitRecord);

    helper.handleExecutionError(context);

    verify(splitRecord).setErrorDetails("Failed to create request for item %s".formatted(itemId));
    verify(splitRecord).setMediatedRequestStatus(MediatedRequestStatusEnum.FAILED);
    verify(batchRequestSplitService).update(splitRequestId, splitRecord);
  }

  @Test
  void initializeFlowExecution_positive() {
    var batchId = UUID.randomUUID();
    var batchSplitId = UUID.randomUUID();
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getBatchSplitRequestId()).thenReturn(batchSplitId);

    helper.initializeFlowExecution(context);

    verify(batchRequestSplitService).updateStatusById(batchSplitId, BatchRequestSplitStatus.IN_PROGRESS);
    verify(batchRequestsService).updateLastProcessedDateById(batchId);
  }
}
