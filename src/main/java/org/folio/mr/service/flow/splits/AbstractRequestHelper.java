package org.folio.mr.service.flow.splits;

import java.util.UUID;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import org.folio.mr.domain.BatchSplitContext;
import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.service.MediatedBatchRequestSplitService;
import org.folio.mr.service.MediatedBatchRequestsService;

@Log4j2
public abstract class AbstractRequestHelper {

  @Setter(onMethod_ = @Autowired)
  private MediatedBatchRequestsService batchRequestsService;

  @Setter(onMethod_ = @Autowired)
  protected MediatedBatchRequestSplitService batchRequestSplitService;

  @Transactional
  public void createRequest(BatchSplitContext context) {
    var splitRequestId = context.getBatchSplitRequestId();
    var splitRequest = batchRequestSplitService.getById(splitRequestId);
    var batchRequest = batchRequestsService.getById(context.getBatchRequestId());

    log.info("execute:: Creating {} request for item with id: {} at service point with id: {}",
      getRequestName(), splitRequest.getItemId(), splitRequest.getPickupServicePointId());

    createRequest(splitRequestId, batchRequest, splitRequest);
    batchRequestsService.updateLastProcessedDateById(context.getBatchRequestId());
  }

  /**
   * Creates a specific type of request for the given splitId, batchRequest and splitRequest.
   *
   * @param batchRequest mediated batch request
   * @param splitRequest mediated batch request split
   */
  protected abstract void createRequest(UUID splitRequestId,
    MediatedBatchRequestDto batchRequest, MediatedBatchRequestDetailDto splitRequest);

  /**
   * Returns a human-readable request type name used for logging.
   *
   * @return request type name
   */
  protected abstract String getRequestName();

  protected void updateBatchRequestSplit(UUID splitRequestId,
    MediatedBatchRequestDetailDto split, Request request) {

    split.setConfirmedRequestId(request.getId());
    if (request.getStatus() != null) {
      split.setRequestStatus(request.getStatus().getValue());
    }
    split.setMediatedRequestStatus(MediatedBatchRequestDetailDto.MediatedRequestStatusEnum.COMPLETED);
    batchRequestSplitService.update(splitRequestId, split);
  }
}
