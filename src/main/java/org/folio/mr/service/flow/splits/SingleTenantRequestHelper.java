package org.folio.mr.service.flow.splits;

import static java.util.Objects.requireNonNull;
import static org.folio.mr.exception.MediatedBatchRequestValidationException.invalidPickupServicePoint;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.folio.mr.domain.dto.MediatedBatchRequestDetailDto;
import org.folio.mr.domain.dto.MediatedBatchRequestDto;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.exception.HoldingNotFoundException;
import org.folio.mr.exception.ItemNotFoundException;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.InventoryService;

/**
 * Helper class for executing transactional single tenant request creation within
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
public class SingleTenantRequestHelper extends AbstractRequestHelper {

  private static final String DEFAULT_FULFILLMENT_PREFERENCE = "Hold Shelf";

  private final InventoryService inventoryService;
  private final CirculationRequestService circulationRequestService;

  @Override
  protected void createRequest(UUID splitRequestId, MediatedBatchRequestDto batchRequest,
    MediatedBatchRequestDetailDto splitRequest) {
    var requestType = findMatchingRequestType(splitRequest).orElseThrow(() ->
      invalidPickupServicePoint(splitRequest.getBatchId(), splitRequest.getPickupServicePointId(), splitRequest.getItemId()));
    var requestPostDto = buildLocalRequestPostDto(splitRequest, batchRequest.getRequestDate(), requestType);
    var requestCreated = circulationRequestService.create(requestPostDto);
    updateBatchRequestSplit(splitRequestId, splitRequest, requestCreated);

    log.info("createSingleTenantRequest:: Created Single Tenant request with id {}, for batch split entity {}",
      requestCreated.getId(), splitRequestId);
  }

  @Override
  protected String getRequestName() {
    return "Single Tenant";
  }

  Optional<Request.RequestTypeEnum> findMatchingRequestType(MediatedBatchRequestDetailDto splitRequest) {
    var requesterUuid = UUID.fromString(splitRequest.getRequesterId());
    var itemUuid = UUID.fromString(requireNonNull(splitRequest.getItemId()));
    var allowedServicePoints = circulationRequestService.getItemRequestAllowedServicePoints(requesterUuid, itemUuid);

    var pickupServicePointId = UUID.fromString(requireNonNull(splitRequest.getPickupServicePointId()));
    if (containsServicePoint(allowedServicePoints.page(), pickupServicePointId)) {
      return Optional.of(Request.RequestTypeEnum.PAGE);
    }

    if (containsServicePoint(allowedServicePoints.hold(), pickupServicePointId)) {
      return Optional.of(Request.RequestTypeEnum.HOLD);
    }

    if (containsServicePoint(allowedServicePoints.recall(), pickupServicePointId)) {
      return Optional.of(Request.RequestTypeEnum.RECALL);
    }

    log.warn("findMatchingRequestType:: Pickup Service point id [{}] is not allowed for item id: [{}]",
      pickupServicePointId, itemUuid);
    return Optional.empty();
  }

  private boolean containsServicePoint(Set<ServicePoint> servicePoints, UUID servicePointId) {
    if (CollectionUtils.isEmpty(servicePoints) || servicePointId == null) {
      return false;
    }

    var servicePointIdStr = servicePointId.toString();
    return servicePoints.stream()
      .map(ServicePoint::getId)
      .anyMatch(servicePointIdStr::equals);
  }

  private Request buildLocalRequestPostDto(MediatedBatchRequestDetailDto splitRequest, Date requestDate,
                                           Request.RequestTypeEnum requestType) {
    var itemId = splitRequest.getItemId();
    var holdingsRecordId = getHoldingIdForItem(itemId);
    var instanceId = getInstanceIdForHolding(holdingsRecordId);

    return new Request()
      .requestLevel(Request.RequestLevelEnum.ITEM)
      .requestType(requestType)
      .itemId(itemId)
      .holdingsRecordId(holdingsRecordId)
      .instanceId(instanceId)
      .requesterId(splitRequest.getRequesterId())
      .fulfillmentPreference(Request.FulfillmentPreferenceEnum.fromValue(DEFAULT_FULFILLMENT_PREFERENCE))
      .pickupServicePointId(splitRequest.getPickupServicePointId())
      .requestDate(requestDate)
      .patronComments(splitRequest.getPatronComments());
  }

  private String getHoldingIdForItem(String itemId) {
    return Optional.ofNullable(inventoryService.fetchItem(itemId))
      .orElseThrow(() -> new ItemNotFoundException(itemId))
      .getHoldingsRecordId();
  }

  private String getInstanceIdForHolding(String holdingId) {
    return Optional.ofNullable(inventoryService.fetchHolding(holdingId))
      .orElseThrow(() -> new HoldingNotFoundException(holdingId))
      .getInstanceId();
  }
}
