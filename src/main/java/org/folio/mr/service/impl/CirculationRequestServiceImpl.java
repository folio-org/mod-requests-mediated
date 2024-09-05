package org.folio.mr.service.impl;

import static org.folio.mr.support.ConversionUtils.asString;

import org.folio.mr.client.CirculationClient;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.service.CirculationRequestService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationRequestServiceImpl implements CirculationRequestService {

  private final CirculationClient circulationClient;

  @Override
  public Request create(MediatedRequestEntity mediatedRequest) {
    log.info("create:: creating circulation request for mediated request {}", mediatedRequest.getId());
    return circulationClient.createRequest(buildRequest(mediatedRequest));
  }

  private static Request buildRequest(MediatedRequestEntity mediatedRequest) {
    return new Request()
      .requestLevel(Request.RequestLevelEnum.fromValue(mediatedRequest.getRequestLevel().getValue()))
      .requestType(Request.RequestTypeEnum.fromValue(mediatedRequest.getRequestType().getValue()))
      .instanceId(asString(mediatedRequest.getInstanceId()))
      .holdingsRecordId(asString(mediatedRequest.getHoldingsRecordId()))
      .itemId(asString(mediatedRequest.getItemId()))
      .requesterId(asString(mediatedRequest.getRequesterId()))
      .fulfillmentPreference(Request.FulfillmentPreferenceEnum.fromValue(mediatedRequest.getFulfillmentPreference().getValue()))
      .pickupServicePointId(asString(mediatedRequest.getPickupServicePointId()))
      .requestDate(mediatedRequest.getRequestDate())
      .deliveryAddressTypeId(asString(mediatedRequest.getDeliveryAddressTypeId()))
      .patronComments(mediatedRequest.getPatronComments());

  }
}
