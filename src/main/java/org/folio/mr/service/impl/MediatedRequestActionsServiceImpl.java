package org.folio.mr.service.impl;

import static org.folio.mr.domain.entity.CombinedMediatedRequestStatus.OPEN_ITEM_ARRIVED;
import static org.folio.mr.domain.entity.MediatedRequestStep.ITEM_ARRIVED;

import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.MediatedRequestsRepository;
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

  @Override
  public MediatedRequestEntity confirmItemArrival(String itemBarcode) {
    log.info("confirmItemArrival:: looking for mediated request with item barcode '{}'", itemBarcode);
    var request = mediatedRequestsRepository.findRequestForItemArrivalConfirmation(itemBarcode)
      .orElseThrow(() -> new EntityNotFoundException(String.format(
        "Mediated request for item with barcode '%s' was not found", itemBarcode)));

    log.info("confirmItemArrival:: mediated request found: {}. Changing its status to '{}'.",
      request::getId, OPEN_ITEM_ARRIVED::getValue);
    request.setStatus(OPEN_ITEM_ARRIVED.getValue());
    request.setMediatedRequestStep(ITEM_ARRIVED.getValue());

     return mediatedRequestsRepository.save(request);
  }

}
