package org.folio.mr.controller;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.mr.domain.MediatedRequestContext;
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
import org.folio.mr.domain.dto.SendItemInTransitResponseRequester;
import org.folio.mr.domain.dto.SendItemInTransitResponseStaffSlipContext;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserPersonal;
import org.folio.mr.rest.resource.MediatedRequestsActionsApi;
import org.folio.mr.service.MediatedRequestActionsService;
import org.folio.mr.service.impl.StaffSlipContextService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@AllArgsConstructor
public class MediatedRequestActionsController implements MediatedRequestsActionsApi {

  private final MediatedRequestActionsService actionsService;
  private final StaffSlipContextService staffSlipContextService;
  private final SystemUserScopedExecutionService systemUserService;

  @Override
  public ResponseEntity<Void> confirmMediatedRequest(UUID mediatedRequestId) {
    log.info("confirmMediatedRequest:: mediatedRequestId={}", mediatedRequestId);
    actionsService.confirm(mediatedRequestId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> declineMediatedRequest(UUID mediatedRequestId) {
    log.info("declineMediatedRequest:: mediatedRequestId={}", mediatedRequestId);
    actionsService.decline(mediatedRequestId);
    return ResponseEntity.noContent().build();
  }

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

    ofNullable(request.getSearchIndex())
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
    MediatedRequestContext context = actionsService.sendItemInTransit(request.getItemBarcode());

    return ResponseEntity.ok(buildSendItemInTransitResponse(context));
  }

  private Date logActionAndGetActionDate(MediatedRequest request) {
    log.info("logActionAndGetActionDate:: creating mediated request workflow log entry " +
        "for request with id: {}", request.getId());

    return actionsService.saveMediatedRequestWorkflowLog(request).getActionDate();
  }

  private SendItemInTransitResponse buildSendItemInTransitResponse(MediatedRequestContext context) {
    MediatedRequest request = context.getRequest();
    MediatedRequestItem item = request.getItem();
    MediatedRequestRequester requester = request.getRequester();
    User user = context.getRequester();
    Date inTransitDate = logActionAndGetActionDate(context.getRequest());

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
      .staffSlipContext(createStaffSlipContext(context))
      .mediatedRequest(new ConfirmItemArrivalResponseMediatedRequest()
        .id(UUID.fromString(request.getId()))
        .status(request.getStatus().getValue()))
      .requester(new SendItemInTransitResponseRequester()
        .id(UUID.fromString(request.getRequesterId()))
        .barcode(requester.getBarcode())
        .firstName(requester.getFirstName())
        .middleName(requester.getMiddleName())
        .lastName(requester.getLastName()));

    ofNullable(user)
      .map(User::getPersonal)
      .map(UserPersonal::getAddresses)
      .filter(not(List::isEmpty))
      .map(List::getFirst)
      .ifPresent(address ->
        response.getRequester()
          .addressLine1(address.getAddressLine1())
          .addressLine2(address.getAddressLine2())
          .city(address.getCity())
          .postalCode(address.getPostalCode())
          .region(address.getRegion())
          .countryId(address.getCountryId()));

    ofNullable(request.getSearchIndex())
      .map(MediatedRequestSearchIndex::getCallNumberComponents)
      .ifPresent(components -> response.getItem().callNumberComponents(
        new MediatedRequestItemCallNumberComponents()
          .prefix(components.getPrefix())
          .callNumber(components.getCallNumber())
          .suffix(components.getSuffix())));

    return response;
  }

  private SendItemInTransitResponseStaffSlipContext createStaffSlipContext(
    MediatedRequestContext context) {

    String lendingTenantId = context.getLendingTenantId();
    if (lendingTenantId == null) {
      log.warn("createStaffSlipContext:: lending tenant ID is null");
      return new SendItemInTransitResponseStaffSlipContext();
    }

    return systemUserService.executeSystemUserScoped(lendingTenantId,
      () -> staffSlipContextService.createStaffSlipContext(context));
  }

}
