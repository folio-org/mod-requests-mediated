package org.folio.mr.service.flow;

import static org.folio.mr.support.ServiceUtils.toStream;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.folio.mr.domain.BatchContext;
import org.folio.mr.domain.dto.IdentifiableMediatedBatchSplit;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto.MediatedRequestStatusEnum;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;

/**
 * Helper class for executing transactional flow control methods within
 * {@link org.folio.spring.FolioExecutionContext} set at the stage level.
 *
 * <p>
 *   Required because placing {@link Transactional} directly on the flow control method
 *   conflicts with {@link org.folio.spring.scope.FolioExecutionContextSetter},
 *   causing database requests to be routed to the last cached tenant ID instead of the correct one.
 * </p>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class BatchFlowHelper {

  private final MediatedBatchRequestsService batchRequestsService;
  private final MediatedBatchRequestSplitService batchRequestSplitService;

  @Transactional
  public void prepareForFlowExecution(BatchContext batchContext) {
    log.info("Initializing batch request processing for batchId: {}", batchContext.getBatchRequestId());
    validateAndInitBatchExecution(batchContext);

    var batchId = batchContext.getBatchRequestId();
    var batchSplitEntityIds = toStream(batchRequestSplitService.getAllByBatchId(batchId))
      .filter(BatchFlowHelper::isNotCompletedSplit)
      .map(IdentifiableMediatedBatchSplit::id)
      .toList();

    batchContext.withBatchSplitEntityIds(batchSplitEntityIds);
  }

  @Transactional
  public void finalizeFlowExecution(BatchContext context) {
    var batchId = context.getBatchRequestId();
    var requestSplits = batchRequestSplitService.getAllByBatchId(batchId);

    if (atLeastOneSplitEntityIsFailed(requestSplits)) {
      log.info("At least one batch split entity is failed, batch with id: {} will be marked as failed", batchId);
      batchRequestsService.updateStatusById(batchId, MediatedRequestStatusEnum.FAILED);
      return;
    }

    if (CollectionUtils.isEmpty(requestSplits) || allSplitEntitiesAreCompleted(requestSplits)) {
      log.info("All batch split entities have completed, batch with id: {} will be marked as completed", batchId);
      batchRequestsService.updateStatusById(batchId, MediatedRequestStatusEnum.COMPLETED);
    }
  }

  @Transactional
  public void handleFailedFlowExecution(BatchContext context) {
    var batchId = context.getBatchRequestId();
    batchRequestsService.updateStatusById(batchId, MediatedRequestStatusEnum.FAILED);

    var errorMessage = context.getBatchRequestFailedMessage();
    if (StringUtils.isNotBlank(errorMessage)) {
      batchRequestSplitService.markNotCompletedRequestsAsFailed(batchId, errorMessage);
    }
  }

  private boolean atLeastOneSplitEntityIsFailed(List<IdentifiableMediatedBatchSplit> requestSplits) {
    return requestSplits.stream()
      .map(IdentifiableMediatedBatchSplit::mediatedBatchRequest)
      .map(MediatedBatchRequestDetailDto::getMediatedRequestStatus)
      .anyMatch(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.FAILED::equals);
  }

  private boolean allSplitEntitiesAreCompleted(List<IdentifiableMediatedBatchSplit> requestSplits) {
    return requestSplits.stream()
      .map(IdentifiableMediatedBatchSplit::mediatedBatchRequest)
      .map(MediatedBatchRequestDetailDto::getMediatedRequestStatus)
      .allMatch(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED::equals);
  }

  private void validateAndInitBatchExecution(BatchContext context) {
    if (context.getDeploymentEnvType() == null) {
      log.error("Environment type is not provided for processing batch request");
      throw new IllegalStateException("No Batch Flow Context parameter for deployment environment type was provided");
    }

    var batchId = context.getBatchRequestId();
    batchRequestsService.updateStatusById(batchId, MediatedRequestStatusEnum.IN_PROGRESS);
  }

  private static boolean isNotCompletedSplit(IdentifiableMediatedBatchSplit split) {
    var requestStatus = split.mediatedBatchRequest().getMediatedRequestStatus();
    return requestStatus != MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED
      && requestStatus != MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.FAILED;
  }
}
