package org.folio.mr.controller;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.dto.ConfirmItemArrivalRequest;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponse;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponseInstance;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponseItem;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponseMediatedRequest;
import org.folio.mr.domain.dto.ConfirmItemArrivalResponseRequester;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.MediatedRequestItemCallNumberComponents;
import org.folio.mr.domain.dto.MediatedRequestRequester;
import org.folio.mr.domain.dto.MediatedRequestSearchIndex;
import org.folio.mr.domain.dto.SendItemInTransitRequest;
import org.folio.mr.domain.dto.SendItemInTransitResponse;
import org.folio.mr.rest.resource.MediatedRequestsActionsApi;
import org.folio.mr.service.MediatedRequestActionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@AllArgsConstructor
public class MediatedRequestActionsController implements MediatedRequestsActionsApi {

  private final MediatedRequestActionsService actionsService;

  @Override
  public ResponseEntity<ConfirmItemArrivalResponse> confirmItemArrival(
    ConfirmItemArrivalRequest request) {

    log.info("confirmItemArrival:: request={}", request);
    MediatedRequest mediatedRequest = actionsService.confirmItemArrival(request.getItemBarcode());

    return ResponseEntity.ok(
      buildConfirmItemArrivalResponse(mediatedRequest, logActionAndGetActionDate(mediatedRequest)));
  }

  private static ConfirmItemArrivalResponse buildConfirmItemArrivalResponse(MediatedRequest request,
    Date arrivalDate) {

    MediatedRequestItem item = request.getItem();
    MediatedRequestRequester requester = request.getRequester();

    ConfirmItemArrivalResponse response = new ConfirmItemArrivalResponse()
      .arrivalDate(arrivalDate)
      .instance(new ConfirmItemArrivalResponseInstance()
        .id(UUID.fromString(request.getInstanceId()))
        .title(request.getInstance().getTitle()))
      .item(new ConfirmItemArrivalResponseItem()
        .id(UUID.fromString(request.getItemId()))
        .barcode(item.getBarcode())
        .enumeration(item.getEnumeration())
        .volume(item.getVolume())
        .chronology(item.getChronology())
        .displaySummary(item.getDisplaySummary())
        .copyNumber(item.getCopyNumber()))
      .mediatedRequest(new ConfirmItemArrivalResponseMediatedRequest()
        .id(UUID.fromString(request.getId()))
        .status(request.getStatus().getValue()))
      .requester(new ConfirmItemArrivalResponseRequester()
        .id(UUID.fromString(request.getRequesterId()))
        .barcode(requester.getBarcode())
        .firstName(requester.getFirstName())
        .middleName(requester.getMiddleName())
        .lastName(requester.getLastName()));

    Optional.ofNullable(request.getSearchIndex())
      .map(MediatedRequestSearchIndex::getCallNumberComponents)
      .ifPresent(components -> response.getItem().callNumberComponents(
        new MediatedRequestItemCallNumberComponents()
          .prefix(components.getPrefix())
          .callNumber(components.getCallNumber())
          .suffix(components.getSuffix())));

    return response;
  }

  @Override
  public ResponseEntity<SendItemInTransitResponse> sendItemInTransit(
    SendItemInTransitRequest request) {

    log.info("sendItemInTransit:: request={}", request);
    MediatedRequest mediatedRequest = actionsService.sendItemInTransit(request.getItemBarcode());

    return ResponseEntity.ok(buildSendItemInTransitResponse(mediatedRequest,
      logActionAndGetActionDate(mediatedRequest)));
  }

  private Date logActionAndGetActionDate(MediatedRequest request) {
    log.info("logActionAndGetActionDate:: creating mediated request workflow log entry " +
        "for request with id: {}", request.getId());

    return actionsService.saveMediatedRequestWorkflowLog(request).getActionDate();
  }

  private static SendItemInTransitResponse buildSendItemInTransitResponse(MediatedRequest request,
    Date inTransitDate) {

    MediatedRequestItem item = request.getItem();
    MediatedRequestRequester requester = request.getRequester();

    SendItemInTransitResponse response = new SendItemInTransitResponse()
      .inTransitDate(inTransitDate)
      .instance(new ConfirmItemArrivalResponseInstance()
        .id(UUID.fromString(request.getInstanceId()))
        .title(request.getInstance().getTitle()))
      .item(new ConfirmItemArrivalResponseItem()
        .id(UUID.fromString(request.getItemId()))
        .barcode(item.getBarcode())
        .enumeration(item.getEnumeration())
        .volume(item.getVolume())
        .chronology(item.getChronology())
        .displaySummary(item.getDisplaySummary())
        .copyNumber(item.getCopyNumber()))
      .mediatedRequest(new ConfirmItemArrivalResponseMediatedRequest()
        .id(UUID.fromString(request.getId()))
        .status(request.getStatus().getValue()))
      .requester(new ConfirmItemArrivalResponseRequester()
        .id(UUID.fromString(request.getRequesterId()))
        .barcode(requester.getBarcode())
        .firstName(requester.getFirstName())
        .middleName(requester.getMiddleName())
        .lastName(requester.getLastName()));

    Optional.ofNullable(request.getSearchIndex())
      .map(MediatedRequestSearchIndex::getCallNumberComponents)
      .ifPresent(components -> response.getItem().callNumberComponents(
        new MediatedRequestItemCallNumberComponents()
          .prefix(components.getPrefix())
          .callNumber(components.getCallNumber())
          .suffix(components.getSuffix())));

    return response;
  }

}
