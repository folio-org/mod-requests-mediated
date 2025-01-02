package org.folio.mr.service.impl;

import static org.folio.mr.support.KafkaEvent.EventType.UPDATED;

import java.util.UUID;

import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
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
    log.info("handle:: processing request event: {}", event::getId);
    if (event.getType() == UPDATED) {
      handleRequestUpdateEvent(event);
    } else {
      log.info("handle:: ignoring event {} of unsupported type: {}", event::getId, event::getType);
    }
    log.info("handle:: request event processed: {}", event::getId);
  }

  private void handleRequestUpdateEvent(KafkaEvent<Request> event) {
    log.info("handleRequestUpdateEvent:: handling request update event: {}", event::getId);
    Request updatedRequest = event.getData().getNewVersion();
    if (updatedRequest == null) {
      log.warn("handleRequestUpdateEvent:: event does not contain new version of request");
      return;
    }
    String requestId = updatedRequest.getId();
    mediatedRequestsRepository.findByConfirmedRequestId(UUID.fromString(requestId))
      .ifPresentOrElse(mediatedRequest -> handleRequestUpdateEvent(mediatedRequest, event),
        () -> log.info("handleRequestUpdateEvent:: request {} not found in mediated requests", requestId));
  }

  private void handleRequestUpdateEvent(MediatedRequestEntity mediatedRequest,
                                        KafkaEvent<Request> event) {
    log.debug("handleRequestUpdateEvent:: mediatedRequest={}", () -> mediatedRequest);
    Request updatedRequest = event.getData().getNewVersion();
    if (updatedRequest.getStatus() == Request.StatusEnum.OPEN_IN_TRANSIT) {
      actionsService.changeStatusToInTransitForApproval(mediatedRequest);
    }
    if (updatedRequest.getStatus() == Request.StatusEnum.CLOSED_CANCELLED) {
      actionsService.cancel(mediatedRequest.getId(), updatedRequest);
    }
  }
}
