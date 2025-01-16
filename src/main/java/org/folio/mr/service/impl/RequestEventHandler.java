package org.folio.mr.service.impl;

import static org.folio.mr.support.KafkaEvent.EventType.UPDATED;

import java.util.UUID;

import org.folio.mr.domain.dto.Request;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.KafkaEventHandler;
import org.folio.mr.service.MediatedRequestActionsService;
import org.folio.mr.support.KafkaEvent;
import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Service
@Log4j2
public class RequestEventHandler implements KafkaEventHandler<Request> {

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final MediatedRequestActionsService actionsService;

  @Override
  public void handle(KafkaEvent<Request> event) {
    log.info("handle:: processing request event: {}", event.getId());
    if (event.getType() == UPDATED) {
      handleRequestUpdateEvent(event);
    } else {
      log.info("handle:: ignoring event {} of unsupported type: {}", event.getId(), event.getType());
    }
    log.info("handle:: request event processed: {}", event.getId());
  }

  private void handleRequestUpdateEvent(KafkaEvent<Request> event) {
    log.info("handleRequestUpdateEvent:: handling request update event: {}", event.getId());
    Request updatedRequest = event.getData().getNewVersion();
    if (updatedRequest == null) {
      log.warn("handleRequestUpdateEvent:: event does not contain new version of request");
      return;
    }

    String requestId = updatedRequest.getId();
    Request.StatusEnum updatedRequestStatus = updatedRequest.getStatus();
    log.info("handleRequestUpdateEvent:: handling update event for request {}, status: {}",
      requestId, updatedRequestStatus);

    var mediatedRequest = mediatedRequestsRepository.findByConfirmedRequestId(
      UUID.fromString(requestId)).orElse(null);
    if (mediatedRequest == null) {
      log.info("handleRequestUpdateEvent:: mediated request not found by confirmed request ID {}",
        requestId);
      return;
    }
    if (updatedRequestStatus == Request.StatusEnum.OPEN_IN_TRANSIT) {
      actionsService.changeStatusToInTransitForApproval(mediatedRequest);
    }
    if (updatedRequestStatus == Request.StatusEnum.OPEN_AWAITING_PICKUP) {
      actionsService.changeStatusToAwaitingPickup(mediatedRequest);
    }
    if (updatedRequestStatus == Request.StatusEnum.CLOSED_CANCELLED) {
      actionsService.cancel(mediatedRequest, updatedRequest);
    }
  }
}
