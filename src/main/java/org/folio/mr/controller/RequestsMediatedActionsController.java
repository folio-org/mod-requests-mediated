package org.folio.mr.controller;

import java.util.Date;

import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;
import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponse;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponseInstance;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponseItem;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponseMediatedRequest;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponseRequester;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.rest.resource.MediatedRequestsActionsApi;
import org.folio.mr.service.MediatedRequestActionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@AllArgsConstructor
public class RequestsMediatedActionsController implements MediatedRequestsActionsApi {

  private final MediatedRequestActionsService mediatedRequestActionsService;

  @Override
  public ResponseEntity<ConfirmItemArrivalResponse> confirmItemArrival(ConfirmItemArrivalRequest request) {
    log.info("confirmItemArrival:: request={}", request);
    MediatedRequestEntity mediatedRequest = mediatedRequestActionsService.confirmItemArrival(
      request.getItemBarcode());

    ConfirmItemArrivalResponse response = new ConfirmItemArrivalResponse()
      .arrivalDate(new Date())
      .instance(new ConfirmItemArrivalResponseInstance()
        .id(mediatedRequest.getInstanceId())
        .title(mediatedRequest.getInstanceTitle()))
      .item(new ConfirmItemArrivalResponseItem()
        .id(mediatedRequest.getItemId())
        .barcode(mediatedRequest.getItemBarcode())
        .effectiveCallNumberString(mediatedRequest.getShelvingOrder()))
      .mediatedRequest(new ConfirmItemArrivalResponseMediatedRequest()
        .id(mediatedRequest.getId())
        .status(mediatedRequest.getStatus()))
      .requester(new ConfirmItemArrivalResponseRequester()
        .id(mediatedRequest.getRequesterId())
        .barcode(mediatedRequest.getRequesterBarcode())
        .name(mediatedRequest.getRequesterLastName() + ", " + mediatedRequest.getRequesterFirstName()));

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> sendItemInTransit(SendItemInTransitRequest mediatedRequest) {
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
