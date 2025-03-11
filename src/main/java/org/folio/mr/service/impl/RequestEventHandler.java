package org.folio.mr.service.impl;

import static org.folio.mr.domain.MediatedRequestStatus.OPEN;
import static org.folio.mr.domain.dto.Request.EcsRequestPhaseEnum.PRIMARY;
import static org.folio.mr.domain.dto.Request.StatusEnum.CLOSED_CANCELLED;
import static org.folio.mr.domain.dto.Request.StatusEnum.CLOSED_FILLED;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_AWAITING_DELIVERY;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_AWAITING_PICKUP;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_IN_TRANSIT;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_NOT_YET_FILLED;
import static org.folio.mr.domain.entity.MediatedRequestStep.AWAITING_DELIVERY;
import static org.folio.mr.domain.entity.MediatedRequestStep.AWAITING_PICKUP;
import static org.folio.mr.domain.entity.MediatedRequestStep.IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.entity.MediatedRequestStep.NOT_YET_FILLED;
import static org.folio.mr.support.KafkaEvent.EventType.UPDATED;

import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.entity.MediatedRequestStep;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.KafkaEventHandler;
import org.folio.mr.service.MediatedRequestActionsService;
import org.folio.mr.service.SearchService;
import org.folio.mr.support.KafkaEvent;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@AllArgsConstructor
@Service
@Log4j2
public class RequestEventHandler implements KafkaEventHandler<Request> {

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final MediatedRequestActionsService actionsService;
  private final SystemUserScopedExecutionService systemUserService;
  private final SearchService searchService;
  private final InventoryService inventoryService;

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

    Request newRequest = event.getData().getNewVersion();
    if (newRequest == null) {
      log.warn("handleRequestUpdateEvent:: event does not contain new version of request");
      return;
    }

    Request.EcsRequestPhaseEnum ecsRequestPhase = newRequest.getEcsRequestPhase();
    log.info("handleRequestUpdateEvent:: updated request ECS phase: {}", ecsRequestPhase);
    if (ecsRequestPhase != PRIMARY) {
      log.info("handleRequestUpdateEvent:: ignoring non-primary request");
      return;
    }

    Request oldRequest = event.getData().getOldVersion();
    if (oldRequest == null) {
      log.warn("handleRequestUpdateEvent:: event does not contain old version of request");
      return;
    }

    String requestId = newRequest.getId();
    var newRequestStatus = newRequest.getStatus();
    var oldRequestStatus = oldRequest.getStatus();
    log.info("handleRequestUpdateEvent:: handling update event for request {}, status: {}, " +
        "old status: {}", requestId, newRequestStatus, oldRequestStatus);

    var mediatedRequest = mediatedRequestsRepository.findByConfirmedRequestId(
      UUID.fromString(requestId)).orElse(null);
    if (mediatedRequest == null) {
      log.info("handleRequestUpdateEvent:: mediated request not found by confirmed request ID {}",
        requestId);
      return;
    } else {
      log.info("handleRequestUpdateEvent:: mediated request found, status {}, step {}",
        mediatedRequest.getMediatedRequestStatus(), mediatedRequest.getMediatedRequestStep());
    }

      boolean wasMediatedRequestUpdated = updateMediatedRequest(mediatedRequest, newRequest);

    // Update statuses
    if (newRequestStatus == OPEN_IN_TRANSIT && oldRequestStatus == OPEN_NOT_YET_FILLED &&
      mediatedRequestStatusEquals(mediatedRequest, OPEN, NOT_YET_FILLED)) {
      actionsService.changeStatusToInTransitForApproval(mediatedRequest);
      wasMediatedRequestUpdated = true;
    }
    if ((newRequestStatus == OPEN_AWAITING_PICKUP || newRequestStatus == OPEN_AWAITING_DELIVERY) &&
      oldRequestStatus == OPEN_IN_TRANSIT &&
      mediatedRequestStatusEquals(mediatedRequest, OPEN, IN_TRANSIT_TO_BE_CHECKED_OUT)
    ) {
      actionsService.changeStatusToAwaitingPickup(mediatedRequest);
      wasMediatedRequestUpdated = true;
    }
    if (newRequestStatus == CLOSED_FILLED &&
      (oldRequestStatus == OPEN_AWAITING_PICKUP || oldRequestStatus == OPEN_AWAITING_DELIVERY) &&
      (mediatedRequestStatusEquals(mediatedRequest, OPEN, AWAITING_PICKUP) ||
        mediatedRequestStatusEquals(mediatedRequest, OPEN, AWAITING_DELIVERY))
    ) {
      actionsService.changeStatusToClosedFilled(mediatedRequest);
      wasMediatedRequestUpdated = true;
    }
    if (newRequestStatus == CLOSED_CANCELLED) {
      actionsService.changeStatusToClosedCanceled(mediatedRequest, newRequest);
      wasMediatedRequestUpdated = true;
    }

    if (wasMediatedRequestUpdated) {
      mediatedRequestsRepository.save(mediatedRequest);
    }
  }

  private boolean mediatedRequestStatusEquals(MediatedRequestEntity mediatedRequest,
    MediatedRequestStatus status, MediatedRequestStep step) {

    log.info("mediatedRequestStatusEquals:: checking mediated request {} status {}, step {}",
      mediatedRequest.getId(), status, step);
    boolean result = mediatedRequest.getMediatedRequestStatus() == status &&
      step.getValue().equals(mediatedRequest.getMediatedRequestStep());
    log.info("mediatedRequestStatusEquals:: result {}", result);
    return result;
  }

  private boolean updateMediatedRequest(MediatedRequestEntity mediatedRequest, Request updatedRequest) {
    String itemId = updatedRequest.getItemId();
    if (mediatedRequest.getItemId() != null || itemId == null) {
      log.info("updateMediatedRequest:: no need to update item info");
      return false;
    }

    log.info("updateMediatedRequest:: updating mediated request item info");
    mediatedRequest.setItemId(UUID.fromString(itemId));
    mediatedRequest.setHoldingsRecordId(UUID.fromString(updatedRequest.getHoldingsRecordId()));

    if (updatedRequest.getItem() != null) {
      mediatedRequest.setItemBarcode(updatedRequest.getItem().getBarcode());
    }

    findItem(itemId).ifPresent(item -> {
      log.info("updateMediatedRequest:: item found, updating mediated request");
      mediatedRequest.setShelvingOrder(item.getEffectiveShelvingOrder());
      ItemEffectiveCallNumberComponents components = item.getEffectiveCallNumberComponents();
      if (components != null) {
        mediatedRequest.setCallNumber(components.getCallNumber());
        mediatedRequest.setCallNumberPrefix(components.getPrefix());
        mediatedRequest.setCallNumberSuffix(components.getSuffix());
      }
    });

    return true;
  }

  private Optional<Item> findItem(String itemId) {
    return searchService.searchItem(itemId)
      .map(searchItem -> systemUserService.executeSystemUserScoped(searchItem.getTenantId(),
        () -> inventoryService.fetchItem(itemId)));
  }

}
