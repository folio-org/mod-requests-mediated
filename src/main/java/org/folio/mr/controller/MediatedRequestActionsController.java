package org.folio.mr.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.folio.mr.domain.MediatedRequestStatus;
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
import org.folio.mr.domain.entity.MediatedRequestWorkflowLog;
import org.folio.mr.repository.MediatedRequestWorkflowLogRepository;
import org.folio.mr.rest.resource.MediatedRequestsActionsApi;
import org.folio.mr.service.MediatedRequestActionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@AllArgsConstructor
public class MediatedRequestActionsController implements MediatedRequestsActionsApi {

  private final MediatedRequestActionsService actionsService;
  private final MediatedRequestWorkflowLogRepository repository;

  @Override
  public ResponseEntity<ConfirmItemArrivalResponse> confirmItemArrival(ConfirmItemArrivalRequest request) {
    log.info("confirmItemArrival:: request={}", request);
    MediatedRequest mediatedRequest = actionsService.confirmItemArrival(request.getItemBarcode());
    //TODO: tmp for testing
    MediatedRequestWorkflowLog log = new MediatedRequestWorkflowLog();
    log.setMediatedRequestId(UUID.fromString(mediatedRequest.getId()));
    log.setMediatedWorkflow(mediatedRequest.getMediatedWorkflow());
    log.setMediatedRequestStep(mediatedRequest.getMediatedRequestStep());
    log.setMediatedRequestStatus(Arrays.stream(MediatedRequestStatus.values())
      .filter(s -> s.name().equals(mediatedRequest.getMediatedRequestStatus().name())).findFirst()
      .orElseThrow());
    MediatedRequestWorkflowLog saved = repository.save(log);

    return ResponseEntity.ok(buildConfirmItemArrivalResponse(mediatedRequest));
  }

  @Override
  public ResponseEntity<Void> sendItemInTransit(SendItemInTransitRequest mediatedRequest) {
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  private static ConfirmItemArrivalResponse buildConfirmItemArrivalResponse(MediatedRequest request) {
    MediatedRequestItem item = request.getItem();
    MediatedRequestRequester requester = request.getRequester();

    ConfirmItemArrivalResponse response = new ConfirmItemArrivalResponse()
      .arrivalDate(new Date())
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
