package org.folio.mr.service.flow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.dto.IdentifiableMediatedBatchSplit;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum;
import org.folio.mr.exception.MediatedBatchRequestNotFoundException;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;

@ExtendWith(MockitoExtension.class)
class BatchFlowHelperTest {

  @InjectMocks private BatchFlowHelper helper;
  @Mock private BatchContext context;
  @Mock private MediatedBatchRequestsService batchRequestsService;
  @Mock private MediatedBatchRequestSplitService batchRequestSplitService;

  @Test
  void prepareForFlowExecution_positive_shouldInitExecutionByModifyingBatchStatus() {
    var batchId = UUID.randomUUID();
    var pendingSplit = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.PENDING);
    var inProgressSplit = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.IN_PROGRESS);
    var completedSplit = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED);
    var failedSplit = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.FAILED);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.ECS);
    when(batchRequestSplitService.getAllByBatchId(batchId))
      .thenReturn(List.of(pendingSplit, inProgressSplit, completedSplit, failedSplit));
    when(context.withBatchSplitEntityIds(anyList())).thenReturn(context);

    helper.prepareForFlowExecution(context);

    verify(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.IN_PROGRESS);
    verify(context).withBatchSplitEntityIds(List.of(pendingSplit.id(), inProgressSplit.id()));
  }

  @Test
  void prepareForFlowExecution_negative_shouldThrowNotFoundExceptionWhenEntityNotFound() {
    var batchId = UUID.randomUUID();
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.ECS);
    doThrow(new MediatedBatchRequestNotFoundException(batchId))
      .when(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.IN_PROGRESS);

    var ex = assertThrows(
      MediatedBatchRequestNotFoundException.class,
      () -> helper.prepareForFlowExecution(context));
    assertTrue(ex.getMessage().contains("Mediated Batch Request with ID [%s] was not found".formatted(batchId)));
    verify(batchRequestSplitService, never()).getAllByBatchId(any(UUID.class));
  }

  @Test
  void prepareForFlowExecution_negative_shouldThrowIllegalStateExceptionWhenNoEnvTypeProvided() {
    var batchId = UUID.randomUUID();

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getDeploymentEnvType()).thenReturn(null);

    var ex = assertThrows(
      IllegalStateException.class,
      () -> helper.prepareForFlowExecution(context));
    assertTrue(ex.getMessage().contains("No Batch Flow Context parameter for deployment environment type was provided"));
    verifyNoInteractions(batchRequestsService, batchRequestSplitService);
  }

  @Test
  void prepareForFlowExecution_positive_shouldSetBatchSplitEntitiesFilteringOnlyPending() {
    var batchId = UUID.randomUUID();
    var split1 = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.PENDING);
    var split2 = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.PENDING);

    when(context.getDeploymentEnvType()).thenReturn(EnvironmentType.ECS);
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getAllByBatchId(batchId)).thenReturn(List.of(split1, split2));
    when(context.withBatchSplitEntityIds(anyList())).thenReturn(context);

    assertDoesNotThrow(() -> helper.prepareForFlowExecution(context));
    verify(context).withBatchSplitEntityIds(argThat(list -> list.size() == 2));
  }


  @ParameterizedTest
  @EnumSource(EnvironmentType.class)
  void prepareForFlowExecution_positive_shouldFilterOutNonFinishedRequests(EnvironmentType envType) {
    var batchId = UUID.randomUUID();

    var pendingSplit = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.PENDING);
    var inProgressSplit = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.IN_PROGRESS);
    var completedSplit = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED);
    var failedSplit = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.FAILED);
    var splitRequests = List.of(pendingSplit, inProgressSplit, completedSplit, failedSplit);

    when(context.getDeploymentEnvType()).thenReturn(envType);
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getAllByBatchId(batchId)).thenReturn(splitRequests);
    when(context.withBatchSplitEntityIds(anyList())).thenReturn(context);

    helper.prepareForFlowExecution(context);
    verify(context).withBatchSplitEntityIds(List.of(pendingSplit.id(), inProgressSplit.id()));
  }

  @Test
  void finalizeFlowExecution_positive_shouldSetStatusToFailedWhenAtLeastOneSplitIsFailed() {
    var batchId = UUID.randomUUID();
    var split1 = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED);
    var split2 = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.FAILED);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getAllByBatchId(batchId)).thenReturn(List.of(split1, split2));

    helper.finalizeFlowExecution(context);

    verify(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.FAILED);
  }

  @Test
  void finalizeFlowExecution_positive_shouldSetStatusToCompletedWhenAllSplitsAreCompleted() {
    var batchId = UUID.randomUUID();
    var split1 = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED);
    var split2 = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getAllByBatchId(batchId)).thenReturn(List.of(split1, split2));

    helper.finalizeFlowExecution(context);

    verify(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.COMPLETED);
  }

  @Test
  void finalizeFlowExecution_positive_shouldSetStatusToCompletedWhenNoSplitsFound() {
    var batchId = UUID.randomUUID();

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getAllByBatchId(batchId)).thenReturn(List.of());

    helper.finalizeFlowExecution(context);

    verify(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.COMPLETED);
  }

  @Test
  void finalizeFlowExecution_positive_shouldNotChangeStatusWhenSplitsAreNotAllCompletedOrFailed() {
    var batchId = UUID.randomUUID();
    var split1 = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED);
    var split2 = batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.PENDING);

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getAllByBatchId(batchId)).thenReturn(List.of(split1, split2));

    helper.finalizeFlowExecution(context);

    verify(batchRequestsService, never()).updateStatusById(any(UUID.class), any(MediatedRequestStatusEnum.class));
  }

  @Test
  void finalizeFlowExecution_negative_shouldThrowExceptionWhenBatchEntityNotFound() {
    var batchId = UUID.randomUUID();
    when(context.getBatchRequestId()).thenReturn(batchId);
    when(batchRequestSplitService.getAllByBatchId(batchId)).thenReturn(List.of());
    doThrow(new MediatedBatchRequestNotFoundException(batchId))
      .when(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.COMPLETED);

    var ex = assertThrows(MediatedBatchRequestNotFoundException.class, () -> helper.finalizeFlowExecution(context));
    assertTrue(ex.getMessage().contains("Mediated Batch Request with ID [%s] was not found".formatted(batchId)));
  }

  @Test
  void execute_positive_shouldSetStatusToFailedAndSaveEntity() {
    var batchId = UUID.randomUUID();

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getBatchRequestFailedMessage()).thenReturn("batch request failed");

    helper.handleFailedFlowExecution(context);

    verify(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.FAILED);
    verify(batchRequestSplitService).markNotCompletedRequestsAsFailed(batchId, "batch request failed");
  }

  @Test
  void execute_positive_shouldSetStatusToFailedForBatchEntityOnly() {
    var batchId = UUID.randomUUID();

    when(context.getBatchRequestId()).thenReturn(batchId);
    when(context.getBatchRequestFailedMessage()).thenReturn("");

    helper.handleFailedFlowExecution(context);

    verify(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.FAILED);
    verify(batchRequestSplitService, never()).markNotCompletedRequestsAsFailed(any(UUID.class), any(String.class));
  }

  @Test
  void execute_negative_shouldThrowExceptionWhenEntityNotFound() {
    var batchId = UUID.randomUUID();
    when(context.getBatchRequestId()).thenReturn(batchId);
    doThrow(new MediatedBatchRequestNotFoundException(batchId))
      .when(batchRequestsService).updateStatusById(batchId, MediatedRequestStatusEnum.FAILED);

    var ex = assertThrows(
      MediatedBatchRequestNotFoundException.class,
      () -> helper.handleFailedFlowExecution(context));
    assertTrue(ex.getMessage().contains("Mediated Batch Request with ID [%s] was not found".formatted(batchId)));
    verify(batchRequestSplitService, never()).markNotCompletedRequestsAsFailed(any(UUID.class), any(String.class));
  }

  private static IdentifiableMediatedBatchSplit batchSplit(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum status) {
    var split = new MediatedBatchRequestDetailDto().mediatedRequestStatus(status);
    return new IdentifiableMediatedBatchSplit(UUID.randomUUID(), split);
  }
}
