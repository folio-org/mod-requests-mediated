package org.folio.mr.service.impl;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT;
import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.domain.entity.MediatedRequestStep.from;

import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.mapper.MediatedRequestMapper;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.service.InventoryService;
import org.folio.mr.service.MediatedRequestActionsService;
import org.springframework.stereotype.Service;

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
    MediatedRequestEntity entity = findMediatedRequestForSendingInTransit(itemBarcode);
    MediatedRequestEntity updatedEntity = updateMediatedRequestStatus(entity, OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT);
    MediatedRequest dto = mediatedRequestMapper.mapEntityToDto(updatedEntity);
    extendMediatedRequest(dto);

    log.debug("sendItemInTransit:: result: {}", dto);
    return dto;
  }

  private MediatedRequestEntity findMediatedRequestForSendingInTransit(String itemBarcode) {
    log.info("findMediatedRequestForSendingInTransit:: looking for mediated request with item barcode '{}'",
      itemBarcode);
    var entity = mediatedRequestsRepository.findRequestForSendingInTransit(itemBarcode)
      .orElseThrow(() -> new EntityNotFoundException(String.format(
        "Mediated request for in transit sending of item with barcode '%s' was not found", itemBarcode)));

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

}
