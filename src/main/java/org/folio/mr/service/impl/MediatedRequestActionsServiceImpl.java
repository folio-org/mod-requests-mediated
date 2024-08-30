package org.folio.mr.service.impl;

import static org.folio.mr.domain.dto.MediatedRequest.StatusEnum.OPEN_ITEM_ARRIVED;
import static org.folio.mr.domain.entity.MediatedRequestStep.ITEM_ARRIVED;

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
    log.info("confirmItemArrival:: looking for mediated request with item barcode '{}'", itemBarcode);
    var requestEntity = mediatedRequestsRepository.findRequestForItemArrivalConfirmation(itemBarcode)
      .orElseThrow(() -> new EntityNotFoundException(String.format(
        "Mediated request for item with barcode '%s' was not found", itemBarcode)));

    log.info("confirmItemArrival:: mediated request found: {}. Changing its status to '{}'.",
      requestEntity::getId, OPEN_ITEM_ARRIVED::getValue);
    requestEntity.setStatus(OPEN_ITEM_ARRIVED.getValue());
    requestEntity.setMediatedRequestStep(ITEM_ARRIVED.getValue());

    MediatedRequestEntity updatedRequestEntity = mediatedRequestsRepository.save(requestEntity);
    MediatedRequest request = mediatedRequestMapper.mapEntityToDto(updatedRequestEntity);

    log.info("confirmItemArrival:: extending mediated request with additional item details");
    Item item = inventoryService.fetchItem(request.getItemId());

    request.getItem()
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .copyNumber(item.getCopyNumber());

    log.debug("confirmItemArrival:: result: {}", request);
    return request;
  }

}
