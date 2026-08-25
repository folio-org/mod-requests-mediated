package org.folio.mr.service.flow.splits;

import static org.folio.mr.domain.BatchRequestSplitStatus.IN_PROGRESS;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto.MediatedRequestStatusEnum;
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
public class BatchSplitFlowHelper {

  private final MediatedBatchRequestsService batchRequestsService;
  private final MediatedBatchRequestSplitService batchRequestSplitService;

  @Transactional
  public void initializeFlowExecution(BatchSplitContext context) {
    var splitRequestId = context.getBatchSplitRequestId();
    batchRequestSplitService.updateStatusById(splitRequestId, IN_PROGRESS);
    batchRequestsService.updateLastProcessedDateById(context.getBatchRequestId());
  }

  @Transactional
  public void handleExecutionError(BatchSplitContext context) {
    var error = context.getExecutionError();
    var splitRequestId = context.getBatchSplitRequestId();
    log.debug("execute:: Handling error for batch split: {}", splitRequestId, error);
    var splitRequest = batchRequestSplitService.getById(splitRequestId);
    var errorMessage = Optional.ofNullable(error.getCause())
      .map(cause -> error.getMessage() + ", cause: " + cause.getMessage())
      .orElse(error.getMessage());

    if (StringUtils.isBlank(errorMessage)) {
      errorMessage = "Failed to create request for item %s".formatted(splitRequest.getItemId());
    }
    log.error("onError:: Batch split entity with id: {} has failed with error: {}",
      splitRequestId, errorMessage);

    splitRequest.setErrorDetails(errorMessage);
    splitRequest.setMediatedRequestStatus(MediatedRequestStatusEnum.FAILED);
    batchRequestSplitService.update(splitRequestId, splitRequest);
    batchRequestsService.updateLastProcessedDateById(context.getBatchRequestId());
  }
}
