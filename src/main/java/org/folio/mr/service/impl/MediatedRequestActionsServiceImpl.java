package org.folio.mr.service.impl;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.CLOSED_CANCELLED;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.CLOSED_DECLINED;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.CLOSED_FILLED;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_AWAITING_DELIVERY;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_NOT_YET_FILLED;
import static org.folio.mr.support.ConversionUtils.asString;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.folio.mr.domain.MediatedRequestContext;
import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.RequestDeliveryAddress;
import org.folio.mr.domain.dto.RequestPickupServicePoint;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserPersonal;
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
import org.folio.mr.service.UserService;
import org.folio.mr.service.ValidatorService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class MediatedRequestActionsServiceImpl implements MediatedRequestActionsService {
  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final InventoryService inventoryService;
  private final UserService userService;
  private final MediatedRequestMapper mediatedRequestMapper;
  private final MediatedRequestWorkflowLogRepository workflowLogRepository;
  private final CirculationRequestService circulationRequestService;
  private final EcsRequestService ecsRequestService;
  private final FolioExecutionContext folioExecutionContext;
  private final SearchService searchService;
  private final SystemUserScopedExecutionService executionService;
  private final ValidatorService validatorService;

  @Override
  public void confirm(UUID id) {
    MediatedRequestEntity mediatedRequest = findMediatedRequest(id);
    log.info("confirm:: found mediated request: {}", id);

    validatorService.validateRequesterForConfirm(mediatedRequest);

    Request request = createRequest(mediatedRequest);
    updateMediatedRequest(mediatedRequest, request);
    log.info("confirm:: mediated request {} was successfully confirmed", id);
  }

  private Request createRequest(MediatedRequestEntity mediatedRequest) {
    log.info("createRequest:: creating request for mediated request: {}", mediatedRequest::getId);
    return localInstanceExists(mediatedRequest) && localItemExists(mediatedRequest)
      ? createLocalRequest(mediatedRequest)
      : createEcsTlr(mediatedRequest);
  }

  private Request createLocalRequest(MediatedRequestEntity mediatedRequest) {
    return circulationRequestService.create(mediatedRequest);
  }

  private Request createEcsTlr(MediatedRequestEntity mediatedRequest) {
    EcsTlr ecsTlr = ecsRequestService.create(mediatedRequest);
    Request primaryRequest = circulationRequestService.get(ecsTlr.getPrimaryRequestId());
    revertPrimaryRequestRequesterInfo(mediatedRequest, primaryRequest);
    return primaryRequest;
  }

  private void revertPrimaryRequestRequesterInfo(MediatedRequestEntity mediatedRequest,
    Request primaryRequest) {

    log.info("revertPrimaryRequestRequesterInfo:: primary request ID {}", primaryRequest::getId);
    // Changing requesterId from fake proxy ID back to the real ID of the secure patron
    primaryRequest.setRequesterId(mediatedRequest.getRequesterId().toString());
    circulationRequestService.update(primaryRequest);
  }

  private void updateMediatedRequest(MediatedRequestEntity mediatedRequest, Request request) {
    log.info("updateMediatedRequest:: updating mediated request {}", mediatedRequest::getId);
    mediatedRequest.setConfirmedRequestId(UUID.fromString(request.getId()));
    if (request.getStatus() == OPEN_NOT_YET_FILLED) {
      log.info("updateMediatedRequest:: changing mediated request status to {}",
        MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
      changeMediatedRequestStatus(mediatedRequest, MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
    }
    updateMediatedRequestItem(mediatedRequest, request);
    mediatedRequestsRepository.save(mediatedRequest);
    log.info("updateMediatedRequest:: mediated request {} updated", mediatedRequest::getId);
  }

  private void updateMediatedRequestItem(MediatedRequestEntity mediatedRequest,
    Request request) {

    var requestItemId = request.getItemId();
    if (requestItemId != null) {
      log.info("updateMediatedRequestItem:: set itemId: {} to mediated request", requestItemId);
      mediatedRequest.setItemId(UUID.fromString(requestItemId));
    }

    var requestItem = request.getItem();
    if (requestItem != null) {
      log.info("updateMediatedRequestItem:: requestItem is present, set barcode: {}",
        requestItem.getBarcode());
      mediatedRequest.setItemBarcode(requestItem.getBarcode());
      extendMediatedRequestWithInventoryItemDetails(mediatedRequest);
    }
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
  public MediatedRequest confirmItemArrival(String itemBarcode) {
    log.info("confirmItemArrival:: item barcode: {}", itemBarcode);
    MediatedRequestEntity mediatedRequestEntity = findMediatedRequestForItemArrival(itemBarcode);
    changeMediatedRequestStatus(mediatedRequestEntity, OPEN_ITEM_ARRIVED);
    mediatedRequestsRepository.save(mediatedRequestEntity);
    MediatedRequest mediatedRequestDto = mediatedRequestMapper.mapEntityToDto(mediatedRequestEntity);
    MediatedRequestContext context = new MediatedRequestContext(mediatedRequestDto);
    findItem(context);
    findRequester(context);
    extendMediatedRequest(context);
    revertConfirmedRequestDeliveryInfo(context);

    log.debug("confirmItemArrival:: result: {}", mediatedRequestDto);
    return mediatedRequestDto;
  }

  private void revertConfirmedRequestDeliveryInfo(MediatedRequestContext context) {
    var mediatedRequest = context.getRequest();
    log.info("revertConfirmedRequestDeliveryInfo:: mediatedRequest: {}", mediatedRequest.getId());

    // Reverting fulfillment preference
    var confirmedRequest = circulationRequestService.get(mediatedRequest.getConfirmedRequestId());
    confirmedRequest.setFulfillmentPreference(Request.FulfillmentPreferenceEnum.fromValue(
      mediatedRequest.getFulfillmentPreference().getValue()));

    // Reverting delivery address info
    var deliveryAddressTypeId = mediatedRequest.getDeliveryAddressTypeId();
    if (deliveryAddressTypeId != null) {
      log.info("revertConfirmedRequestDeliveryInfo:: " +
          "updating deliveryAddressTypeId; confirmed request: {}, mediated request: {}",
        confirmedRequest::getId, mediatedRequest::getId);
      confirmedRequest.setDeliveryAddressTypeId(deliveryAddressTypeId);

      var deliveryAddress = ofNullable(context.getRequester())
        .map(User::getPersonal)
        .map(UserPersonal::getAddresses)
        .map(Collection::stream)
        .flatMap(addresses -> addresses
          .filter(address -> deliveryAddressTypeId.equals(address.getAddressTypeId()))
          .findFirst())
        .map(address -> new RequestDeliveryAddress()
          .addressLine1(address.getAddressLine1())
          .addressLine2(address.getAddressLine2())
          .city(address.getCity())
          .postalCode(address.getPostalCode())
          .region(address.getRegion())
          .countryId(address.getCountryId()));

      if (deliveryAddress.isPresent()) {
        log.info("revertConfirmedRequestDeliveryInfo:: " +
            "updating deliveryAddress; confirmed request: {}, mediated request: {}",
          confirmedRequest::getId, mediatedRequest::getId);
        confirmedRequest.setDeliveryAddress(deliveryAddress.get());
      }
    }

    // Reverting pickup service point
    confirmedRequest.setPickupServicePointId(mediatedRequest.getPickupServicePointId());
    var mediatedRequestPickupServicePoint = mediatedRequest.getPickupServicePoint();
    if (mediatedRequestPickupServicePoint != null) {
      log.info("revertConfirmedRequestDeliveryInfo:: " +
          "updating pickupServicePoint; confirmed request: {}, mediated request: {}",
        confirmedRequest::getId, mediatedRequest::getId);
      confirmedRequest.setPickupServicePoint(new RequestPickupServicePoint()
        .name(mediatedRequestPickupServicePoint.getName())
        .code(mediatedRequestPickupServicePoint.getCode())
        .discoveryDisplayName(mediatedRequestPickupServicePoint.getDiscoveryDisplayName())
        .pickupLocation(mediatedRequestPickupServicePoint.getPickupLocation()));
    }
    circulationRequestService.update(confirmedRequest);
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

    log.info("findMediatedRequestForItemArrival:: mediated request found: {}", entity.getId());
    return entity;
  }

  @Override
  public MediatedRequestContext sendItemInTransit(String itemBarcode) {
    log.info("sendItemInTransit:: item barcode: {}", itemBarcode);
    var entity = findMediatedRequestForSendingInTransit(itemBarcode);
    changeMediatedRequestStatus(entity, OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT);
    mediatedRequestsRepository.save(entity);
    var dto = mediatedRequestMapper.mapEntityToDto(entity);
    MediatedRequestContext context = new MediatedRequestContext(dto);
    findItem(context);
    findRequester(context);
    extendMediatedRequest(context);

    log.debug("sendItemInTransit:: result: {}", dto);

    return context;
  }

  private MediatedRequestEntity findMediatedRequestForSendingInTransit(String itemBarcode) {
    log.info("findMediatedRequestForSendingInTransit:: looking for mediated " +
        "request with item barcode '{}'", itemBarcode);
    var entity = mediatedRequestsRepository.findRequestForSendingInTransit(itemBarcode)
      .orElseThrow(() -> ExceptionFactory.notFound(format(
        "Send item in transit: mediated request for item '%s' was not found",
        itemBarcode)));

    log.info("findMediatedRequestForSendingInTransit:: mediated request found: {}", entity.getId());

    return entity;
  }

  private void findItem(MediatedRequestContext context) {
    searchService.searchItem(context.getRequest().getItemId())
      .map(ConsortiumItem::getTenantId)
      .map(context::setLendingTenantId)
      .ifPresent(this::fetchItem);
  }

  private void findRequester(MediatedRequestContext context) {
    context.setRequester(userService.fetchUser(context.getRequest().getRequesterId()));
  }

  private void fetchItem(MediatedRequestContext context) {
    String itemId = context.getRequest().getItemId();
    Item item = executionService.executeSystemUserScoped(context.getLendingTenantId(),
      () -> inventoryService.fetchItem(itemId));

    if (item != null) {
      context.setItem(item);
    } else {
      throw ExceptionFactory.notFound(format("Item %s not found", itemId));
    }
  }

  private void extendMediatedRequest(MediatedRequestContext context) {
    Item item = context.getItem();
    if (item == null) {
      log.warn("extendMediatedRequest:: item is null");
      return;
    }

    log.info("extendMediatedRequest:: extending mediated request with additional item details");
    context.getRequest().getItem()
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .copyNumber(item.getCopyNumber());
  }

  private void extendMediatedRequestWithInventoryItemDetails(MediatedRequestEntity mediatedRequestEntity) {
    log.info("extendMediatedRequestWithInventoryItemDetails:: extending mediated request " +
      "with additional item details");

    searchService.searchItem(mediatedRequestEntity.getItemId().toString())
      .ifPresent(searchItem -> executionService.executeSystemUserScoped(searchItem.getTenantId(),
        () -> populateMediatedRequestWithItemDetails(mediatedRequestEntity)));
  }

  private MediatedRequestEntity populateMediatedRequestWithItemDetails(
    MediatedRequestEntity mediatedRequestEntity) {

    Item item = inventoryService.fetchItem(mediatedRequestEntity.getItemId().toString());
    if (item == null) {
      throw ExceptionFactory.notFound(format("Item %s not found", mediatedRequestEntity.getItemId()));
    }
    log.info("populateMediatedRequestWithItemDetails:: item found, updating mediated request");
    mediatedRequestEntity.setShelvingOrder(item.getEffectiveShelvingOrder());
    ItemEffectiveCallNumberComponents components = item.getEffectiveCallNumberComponents();
    if (components != null) {
      mediatedRequestEntity.setCallNumber(components.getCallNumber());
      mediatedRequestEntity.setCallNumberPrefix(components.getPrefix());
      mediatedRequestEntity.setCallNumberSuffix(components.getSuffix());
    }
    String holdingsRecordId = item.getHoldingsRecordId();
    if (holdingsRecordId != null) {
      mediatedRequestEntity.setHoldingsRecordId(UUID.fromString(holdingsRecordId));
    }

    return mediatedRequestEntity;
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
    log.info("decline:: mediated request {} was successfully declined", id);
  }

  private void declineRequest(MediatedRequestEntity request) {
    if (request.getMediatedRequestStatus() != MediatedRequestStatus.NEW ||
      !MediatedRequestStep.AWAITING_CONFIRMATION.getValue().equals(request.getMediatedRequestStep()))
    {
      throw ExceptionFactory.validationError("Mediated request status should be 'New - Awaiting conformation'");
    }
    request.setMediatedWorkflow(MediatedRequestWorkflow.PRIVATE_REQUEST.getValue());
    changeMediatedRequestStatus(request, CLOSED_DECLINED);
    mediatedRequestsRepository.save(request);
  }

  @Override
  public void changeStatusToInTransitForApproval(MediatedRequestEntity request) {
    log.info("changeStatusToInTransitForApproval:: request id: {}", request.getId());
    changeMediatedRequestStatus(request, OPEN_IN_TRANSIT_FOR_APPROVAL);
  }

  @Override
  public void changeStatusToAwaitingPickup(MediatedRequestEntity request) {
    log.info("changeStatusToAwaitingPickup:: request id: {}", request.getId());
    changeMediatedRequestStatus(request, OPEN_AWAITING_PICKUP);
  }

  @Override
  public void changeStatusToAwaitingDelivery(MediatedRequestEntity request) {
    log.info("changeStatusToAwaitingDelivery:: request id: {}", request.getId());
    changeMediatedRequestStatus(request, OPEN_AWAITING_DELIVERY);
  }

  @Override
  public void changeStatusToClosedFilled(MediatedRequestEntity request) {
    log.info("changeStatusToClosedFilled:: request id: {}", request.getId());
    changeMediatedRequestStatus(request, CLOSED_FILLED);
  }

  @Override
  public void changeStatusToClosedCanceled(MediatedRequestEntity request, Request confirmedRequest) {
    log.info("changeStatusToClosedCanceled:: request id: {}", request.getId());
    request.setCancellationReasonId(UUID.fromString(confirmedRequest.getCancellationReasonId()));
    request.setCancelledDate(confirmedRequest.getCancelledDate());
    request.setCancelledByUserId(UUID.fromString(confirmedRequest.getCancelledByUserId()));
    changeMediatedRequestStatus(request, CLOSED_CANCELLED);
  }

  private void changeMediatedRequestStatus(MediatedRequestEntity request,
    MediatedRequest.StatusEnum newStatus) {

    log.info("updateMediatedRequestStatus:: changing mediated request status from '{}' to '{}'",
      request.getStatus(), newStatus.getValue());
    request.setStatus(newStatus.getValue());
    request.setMediatedRequestStatus(MediatedRequestStatus.from(newStatus));
    request.setMediatedRequestStep(MediatedRequestStep.from(newStatus).getValue());
  }

  private MediatedRequestEntity findMediatedRequest(UUID id) {
    log.info("findMediatedRequest:: looking for mediated request: {}", id);
    return mediatedRequestsRepository.findById(id)
      .orElseThrow(() -> ExceptionFactory.notFound("Mediated request was not found: " + id));
  }
}
