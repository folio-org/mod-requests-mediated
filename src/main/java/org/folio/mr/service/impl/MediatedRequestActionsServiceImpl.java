package org.folio.mr.service.impl;

import static java.lang.String.format;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_NOT_YET_FILLED;
import static org.folio.mr.support.ConversionUtils.asString;

import java.util.List;
import java.util.UUID;

import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.RequestDeliveryAddress;
import org.folio.mr.domain.dto.RequestPickupServicePoint;
import org.folio.mr.domain.dto.SearchInstance;
import org.folio.mr.domain.dto.SearchItem;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.entity.MediatedRequestStep;
import org.folio.mr.domain.entity.MediatedRequestWorkflow;
import org.folio.mr.domain.entity.MediatedRequestWorkflowLog;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.exception.ExceptionFactory;
import org.folio.mr.repository.MediatedRequestWorkflowLogRepository;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.EcsRequestService;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedRequestActionsService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class MediatedRequestActionsServiceImpl implements MediatedRequestActionsService {

  private static final String INTERIM_SERVICE_POINT_ID = "32c6f0c7-26e4-4350-8c29-1e11c2e3efc4";
  private static final String INTERIM_SERVICE_POINT_NAME = "Interim service point";
  private static final String INTERIM_SERVICE_POINT_CODE = "interimsp";
  private static final String INTERIM_SERVICE_POINT_DISCOVERY_DISPLAY_NAME= "Interim service point";

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final InventoryService inventoryService;
  private final MediatedRequestMapper mediatedRequestMapper;
  private final MediatedRequestWorkflowLogRepository workflowLogRepository;
  private final CirculationRequestService circulationRequestService;
  private final EcsRequestService ecsRequestService;
  private final FolioExecutionContext folioExecutionContext;
  private final SearchService searchService;
  private final SystemUserScopedExecutionService executionService;

  @Override
  public void confirm(UUID id) {
    MediatedRequestEntity mediatedRequest = findMediatedRequest(id);
    log.info("confirm:: found mediated request: {}", id);
    createRequest(mediatedRequest);
    log.info("confirm:: mediated request {} was successfully confirmed", id);
  }

  private void createRequest(MediatedRequestEntity mediatedRequest) {
    if (localInstanceExists(mediatedRequest) && localItemExists(mediatedRequest)) {
      createLocalRequest(mediatedRequest);
    } else {
      createEcsTlr(mediatedRequest);
    }
    log.info("createRequest:: request created: {}", mediatedRequest::getConfirmedRequestId);
  }

  private void createLocalRequest(MediatedRequestEntity mediatedRequest) {
    Request localRequest = circulationRequestService.create(mediatedRequest);
    updateLocalRequest(localRequest);
    updateMediatedRequest(mediatedRequest, localRequest);
  }

  private void updateLocalRequest(Request request) {
    log.info("updateLocalRequest:: updating local request {}", request::getId);
    changeFulfillmentPreferenceToInterimServicePoint(request);
    circulationRequestService.update(request);
  }

  private void createEcsTlr(MediatedRequestEntity mediatedRequest) {
    EcsTlr ecsTlr = ecsRequestService.create(mediatedRequest);
    Request primaryRequest = circulationRequestService.get(ecsTlr.getPrimaryRequestId());
    updatePrimaryRequest(mediatedRequest, primaryRequest);
    updateMediatedRequest(mediatedRequest, primaryRequest);
  }

  private void updatePrimaryRequest(MediatedRequestEntity mediatedRequest, Request primaryRequest) {
    log.info("updatePrimaryRequest:: updating primary request {}", primaryRequest::getId);
    // Changing requesterId from fake proxy ID back to the real ID of the secure patron
    primaryRequest.setRequesterId(mediatedRequest.getRequesterId().toString());
    changeFulfillmentPreferenceToInterimServicePoint(primaryRequest);
    circulationRequestService.update(primaryRequest);
  }

  private static void changeFulfillmentPreferenceToInterimServicePoint(Request request) {
    log.info("changeFulfillmentPreferenceToInterimServicePoint:: updating fulfillment preference");
    request.setFulfillmentPreference(Request.FulfillmentPreferenceEnum.HOLD_SHELF);
    request.setDeliveryAddress(null);
    request.setDeliveryAddressTypeId(null);
    request.setPickupServicePointId(INTERIM_SERVICE_POINT_ID);
    request.setPickupServicePoint(new RequestPickupServicePoint()
      .name(INTERIM_SERVICE_POINT_NAME)
      .code(INTERIM_SERVICE_POINT_CODE)
      .discoveryDisplayName(INTERIM_SERVICE_POINT_DISCOVERY_DISPLAY_NAME)
      .pickupLocation(true));
  }

  private void updateMediatedRequest(MediatedRequestEntity mediatedRequest, Request request) {
    log.info("updateMediatedRequest:: updating mediated request {}", mediatedRequest::getId);
    mediatedRequest.setConfirmedRequestId(UUID.fromString(request.getId()));
    if (request.getStatus() == OPEN_NOT_YET_FILLED) {
      log.info("updateMediatedRequest:: changing mediated request status to {}", OPEN_NOT_YET_FILLED);
      mediatedRequest.setStatus(OPEN_NOT_YET_FILLED.getValue());
    }
    mediatedRequestsRepository.save(mediatedRequest);
  }

  private boolean localInstanceExists(MediatedRequestEntity mediatedRequest) {
    String instanceId = mediatedRequest.getInstanceId().toString();
    log.info("localInstanceExists:: searching for instance {} in local tenant", instanceId);

    var instanceFound = inventoryService.fetchInstance(instanceId) != null;
    log.info("localInstanceExists:: instanceFound: {}", instanceFound);
    return instanceFound;
  }

  private boolean localItemExists(MediatedRequestEntity mediatedRequest) {
    String instanceId = mediatedRequest.getInstanceId().toString();
    log.info("localItemExists:: searching for items of instance {} in local tenant", instanceId);
    String itemId = asString(mediatedRequest.getItemId());
    String localTenantId = folioExecutionContext.getTenantId();

    List<String> localItemIds = searchService.searchItems(instanceId, localTenantId)
      .stream()
      .map(ConsortiumItem::getId)
      .toList();

    log.info("localItemExists:: found {} items in local tenant", localItemIds.size());
    log.debug("localItemExists:: itemId={}, localItemIds={}", itemId, localItemIds);

    return (itemId != null && localItemIds.contains(itemId))
      || (itemId == null && !localItemIds.isEmpty());
  }

  @Override
  public void changeStatusToInTransitForApproval(MediatedRequestEntity request) {
    log.info("changeStatusToInTransitForApproval:: request id: {}", request.getId());
    request.setMediatedRequestStatus(MediatedRequestStatus.OPEN);
    updateMediatedRequestStatus(request, OPEN_IN_TRANSIT_FOR_APPROVAL);
  }

  @Override
  public MediatedRequest confirmItemArrival(String itemBarcode) {
    log.info("confirmItemArrival:: item barcode: {}", itemBarcode);
    MediatedRequestEntity entity = findMediatedRequestForItemArrival(itemBarcode);
    MediatedRequestEntity updatedEntity = updateMediatedRequestStatus(entity, OPEN_ITEM_ARRIVED);
    MediatedRequest dto = mediatedRequestMapper.mapEntityToDto(updatedEntity);
    extendMediatedRequest(dto);
    updatePrimaryRequest(dto);

    log.debug("confirmItemArrival:: result: {}", dto);
    return dto;
  }

  private void updatePrimaryRequest(MediatedRequest medRequest) {
    log.info("updatePrimaryRequest:: medRequest: {}", medRequest.getId());
    var primaryRequest = circulationRequestService.get(medRequest.getConfirmedRequestId());
    primaryRequest.setFulfillmentPreference(Request.FulfillmentPreferenceEnum.fromValue(
      medRequest.getFulfillmentPreference().getValue()));
    var deliveryAddress = medRequest.getDeliveryAddress();
    if (deliveryAddress != null) {
      log.info("updatePrimaryRequest:: updating deliveryAddress for request: {}", medRequest.getId());
      primaryRequest.setDeliveryAddress(new RequestDeliveryAddress()
        .region(deliveryAddress.getRegion())
        .city(deliveryAddress.getCity())
        .countryId(deliveryAddress.getCountryId())
        .addressTypeId(deliveryAddress.getAddressTypeId())
        .addressLine1(deliveryAddress.getAddressLine1())
        .addressLine2(deliveryAddress.getAddressLine2())
        .postalCode(deliveryAddress.getPostalCode()));
    }
    primaryRequest.setPickupServicePointId(medRequest.getPickupServicePointId());
    var medRequestPickupServicePoint = medRequest.getPickupServicePoint();
    if (medRequestPickupServicePoint != null) {
      log.info("updatePrimaryRequest:: updating pickupServicePoint for primary request: {}", medRequest.getId());
      primaryRequest.setPickupServicePoint(new RequestPickupServicePoint()
        .name(medRequestPickupServicePoint.getName())
        .code(medRequestPickupServicePoint.getCode())
        .discoveryDisplayName(medRequestPickupServicePoint.getDiscoveryDisplayName())
        .pickupLocation(medRequestPickupServicePoint.getPickupLocation()));
    }
    circulationRequestService.update(primaryRequest);
  }

  @Override
  public MediatedRequestWorkflowLog saveMediatedRequestWorkflowLog(MediatedRequest request) {
    return workflowLogRepository.save(buildMediatedRequestWorkflowLog(request));
  }

  private MediatedRequestEntity findMediatedRequestForItemArrival(String itemBarcode) {
    log.info("findMediatedRequestForItemArrival:: looking for mediated request with item barcode '{}'",
      itemBarcode);

    var entity = mediatedRequestsRepository.findRequestForItemArrivalConfirmation(itemBarcode)
      .orElseThrow(() -> ExceptionFactory.notFound(format(
        "Mediated request for arrival confirmation of item with barcode '%s' was not found", itemBarcode)));

    log.info("findMediatedRequestForItemArrival:: mediated request found: {}", entity::getId);
    return entity;
  }

  @Override
  public MediatedRequest sendItemInTransit(String itemBarcode) {
    log.info("sendItemInTransit:: item barcode: {}", itemBarcode);
    var entity = findMediatedRequestForSendingInTransit(itemBarcode);
    var updatedEntity = updateMediatedRequestStatus(entity, OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT);
    var dto = mediatedRequestMapper.mapEntityToDto(updatedEntity);
    extendMediatedRequest(dto);
    log.debug("sendItemInTransit:: result: {}", dto);

    return dto;
  }

  private MediatedRequestEntity findMediatedRequestForSendingInTransit(String itemBarcode) {
    log.info("findMediatedRequestForSendingInTransit:: looking for mediated " +
        "request with item barcode '{}'", itemBarcode);
    var entity = mediatedRequestsRepository.findRequestForSendingInTransit(itemBarcode)
      .orElseThrow(() -> ExceptionFactory.notFound(format(
        "Send item in transit: mediated request for item '%s' was not found",
        itemBarcode)));

    log.info("findMediatedRequestForSendingInTransit:: mediated request found: {}", entity::getId);

    return entity;
  }

  @Override
  public void changeStatusToAwaitingPickup(MediatedRequestEntity request) {
    log.info("changeStatusToAwaitingPickup:: request id: {}", request.getId());
    request.setMediatedRequestStatus(MediatedRequestStatus.OPEN);
    updateMediatedRequestStatus(request, OPEN_AWAITING_PICKUP);
  }

  private MediatedRequestEntity updateMediatedRequestStatus(MediatedRequestEntity request,
    MediatedRequest.StatusEnum newStatus) {

    log.info("updateMediatedRequestStatus:: changing mediated request status from '{}' to '{}'",
      request.getStatus(), newStatus.getValue());
    request.setStatus(newStatus.getValue());
    request.setMediatedRequestStep(MediatedRequestStep.from(newStatus).getValue());

    return mediatedRequestsRepository.save(request);
  }

  private void extendMediatedRequest(MediatedRequest request) {
    log.info("extendMediatedRequest:: extending mediated request with additional item details");

    searchService.searchInstance(request.getInstanceId())
      .map(SearchInstance::getItems)
      .stream()
      .flatMap(List::stream)
      .filter(searchItem -> searchItem.getId().equals(request.getItemId()))
      .findFirst()
      .ifPresent(searchItem -> fillItemDetailsFromInventoryItem(request, searchItem));
  }

  private void fillItemDetailsFromInventoryItem(MediatedRequest request, SearchItem searchItem) {
    String tenantId = searchItem.getTenantId();
    log.info("fillItemDetailsFromInventoryItem:: fetching item from tenant: {}", tenantId);
    executionService.executeAsyncSystemUserScoped(tenantId, () -> {
      Item item = inventoryService.fetchItem(request.getItemId());
      if (item == null) {
        throw ExceptionFactory.notFound(format("Item %s not found", request.getItemId()));
      } else {
        request.getItem()
          .enumeration(item.getEnumeration())
          .volume(item.getVolume())
          .chronology(item.getChronology())
          .displaySummary(item.getDisplaySummary())
          .copyNumber(item.getCopyNumber());
      }
    });
  }

  private static MediatedRequestWorkflowLog buildMediatedRequestWorkflowLog(
    MediatedRequest request) {

    MediatedRequestWorkflowLog log = new MediatedRequestWorkflowLog();
    log.setMediatedRequestId(UUID.fromString(request.getId()));
    log.setMediatedRequestStep(request.getMediatedRequestStep());
    log.setMediatedRequestStatus(MediatedRequestStatus.fromValue(request.getMediatedRequestStatus()
      .getValue()));
    log.setMediatedWorkflow(request.getMediatedWorkflow());

    return log;
  }

  @Override
  public void decline(UUID id) {
    log.info("decline:: looking for mediated request: {}", id);
    MediatedRequestEntity mediatedRequest = findMediatedRequest(id);
    log.debug("decline:: mediatedRequest: {}", mediatedRequest);

    declineRequest(mediatedRequest);
    mediatedRequestsRepository.save(mediatedRequest);
    log.info("decline:: mediated request {} was successfully declined", id);
  }

  @Override
  public void cancel(MediatedRequestEntity mediatedRequest, Request confirmedRequest) {
    log.info("cancel:: mediatedRequest: {}", mediatedRequest::getId);
    mediatedRequest.setStatus(MediatedRequest.StatusEnum.CLOSED_CANCELLED.getValue());
    mediatedRequest.setCancellationReasonId(UUID.fromString(confirmedRequest.getCancellationReasonId()));
    mediatedRequest.setCancelledDate(confirmedRequest.getCancelledDate());
    mediatedRequest.setCancelledByUserId(UUID.fromString(confirmedRequest.getCancelledByUserId()));
    mediatedRequestsRepository.save(mediatedRequest);
    log.info("cancel:: mediated request {} was successfully cancelled", mediatedRequest::getId);
  }

  private void declineRequest(MediatedRequestEntity request) {
    if (request.getMediatedRequestStatus() != MediatedRequestStatus.NEW ||
      !MediatedRequestStep.AWAITING_CONFIRMATION.getValue().equals(request.getMediatedRequestStep()))
    {
      throw ExceptionFactory.validationError("Mediated request status should be 'New - Awaiting conformation'");
    }
    request.setMediatedRequestStatus(MediatedRequestStatus.CLOSED);
    request.setStatus(MediatedRequest.StatusEnum.CLOSED_DECLINED.getValue());
    request.setMediatedRequestStep(MediatedRequestStep.DECLINED.getValue());
    request.setMediatedWorkflow(MediatedRequestWorkflow.PRIVATE_REQUEST.getValue());
  }

  private MediatedRequestEntity findMediatedRequest(UUID id) {
    log.info("findMediatedRequest:: looking for mediated request: {}", id);
    return mediatedRequestsRepository.findById(id)
      .orElseThrow(() -> ExceptionFactory.notFound("Mediated request was not found: " + id));
  }
}
