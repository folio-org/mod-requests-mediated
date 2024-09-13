package org.folio.mr.service.impl;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.domain.entity.MediatedRequestStep.from;
import static org.folio.mr.support.ConversionUtils.asString;

import java.util.List;
import java.util.UUID;

import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.EcsTlr;
import java.util.UUID;

import org.folio.mr.domain.MediatedRequestStatus;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.entity.MediatedRequestWorkflowLog;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestWorkflowLogRepository;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.CirculationRequestService;
import org.folio.mr.service.EcsRequestService;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedRequestActionsService;
import org.folio.mr.service.SearchService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class MediatedRequestActionsServiceImpl implements MediatedRequestActionsService {

  private final MediatedRequestsRepository mediatedRequestsRepository;
  private final InventoryService inventoryService;
  private final MediatedRequestMapper mediatedRequestMapper;
  private final MediatedRequestWorkflowLogRepository workflowLogRepository;
  private final CirculationRequestService circulationRequestService;
  private final EcsRequestService ecsRequestService;
  private final FolioExecutionContext folioExecutionContext;
  private final SearchService searchService;

  @Override
  public void confirm(UUID id) {
    MediatedRequestEntity mediatedRequest = findMediatedRequest(id);
    createRequest(mediatedRequest);
    log.info("confirm:: request created: {}", mediatedRequest.getConfirmedRequestId());
    mediatedRequestsRepository.save(mediatedRequest);
    log.info("confirm:: mediated request {} was successfully confirmed", id);
  }

  private MediatedRequestEntity findMediatedRequest(UUID id) {
    log.info("findMediatedRequest:: looking for mediated request: {}", id);
    return mediatedRequestsRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Mediated request was not found: " + id));
  }

  private void createRequest(MediatedRequestEntity mediatedRequest) {
    if (localInstanceExists(mediatedRequest) && localItemExists(mediatedRequest)) {
      Request request = circulationRequestService.create(mediatedRequest);
      mediatedRequest.setConfirmedRequestId(UUID.fromString(request.getId()));
    } else {
      EcsTlr ecsTlr = ecsRequestService.create(mediatedRequest);
      mediatedRequest.setConfirmedRequestId(UUID.fromString(ecsTlr.getPrimaryRequestId()));
    }
  }

  @Override
  public MediatedRequest confirmItemArrival(String itemBarcode) {
    log.info("confirmItemArrival:: item barcode: {}", itemBarcode);
    MediatedRequestEntity entity = findMediatedRequestForItemArrival(itemBarcode);
    MediatedRequestEntity updatedEntity = updateMediatedRequestStatus(entity, OPEN_ITEM_ARRIVED);
    MediatedRequest dto = mediatedRequestMapper.mapEntityToDto(updatedEntity);
    extendMediatedRequest(dto);

    log.debug("confirmItemArrival:: result: {}", dto);
    return dto;
  }

  @Override
  public MediatedRequestWorkflowLog saveMediatedRequestWorkflowLog(MediatedRequest request) {
    return workflowLogRepository.save(buildMediatedRequestWorkflowLog(request));
  }

  private MediatedRequestEntity findMediatedRequestForItemArrival(String itemBarcode) {
    log.info("findMediatedRequestForItemArrival:: looking for mediated request with item barcode '{}'",
      itemBarcode);

    var entity = mediatedRequestsRepository.findRequestForItemArrivalConfirmation(itemBarcode)
      .orElseThrow(() -> new EntityNotFoundException(String.format(
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
      .orElseThrow(() -> new EntityNotFoundException(String.format(
        "Mediated request for in transit sending of item with barcode '%s' was not found",
          itemBarcode)));

    log.info("findMediatedRequestForSendingInTransit:: mediated request found: {}", entity::getId);

    return entity;
  }

  private MediatedRequestEntity updateMediatedRequestStatus(MediatedRequestEntity request,
    MediatedRequest.StatusEnum newStatus) {

    log.info("updateMediatedRequestStatus:: changing mediated request status from '{}' to '{}'",
      request::getStatus, newStatus::getValue);
    request.setStatus(newStatus.getValue());
    request.setMediatedRequestStep(from(newStatus).getValue());

    return mediatedRequestsRepository.save(request);
  }

  private void extendMediatedRequest(MediatedRequest request) {
    log.info("extendMediatedRequest:: extending mediated request with additional item details");
    Item item = inventoryService.fetchItem(request.getItemId());

    request.getItem()
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .copyNumber(item.getCopyNumber());
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

  private boolean localInstanceExists(MediatedRequestEntity mediatedRequest) {
    final String instanceId = mediatedRequest.getInstanceId().toString();
    log.info("localInstanceExists:: searching for instance {} in local tenant", instanceId);
    try {
      inventoryService.fetchInstance(instanceId);
      log.info("localInstanceExists:: instance found");
      return true;
    } catch (FeignException.NotFound e) {
      log.info("localInstanceExists:: instance not found");
      return false;
    }
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
}
