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
  public Request get(String id) {
    log.info("get:: get circulation request by id {}", id);
    return circulationClient.getRequest(id);
  }

  @Override
  public Request create(MediatedRequestEntity mediatedRequest, String pickupServicePointId) {
    log.info("create:: creating circulation request for mediated request {}", mediatedRequest.getId());
    return circulationClient.createRequest(buildRequest(mediatedRequest, pickupServicePointId));
  }

  private static Request buildRequest(MediatedRequestEntity mediatedRequest,
    String pickupServicePointId) {

    return new Request()
      .requestLevel(Request.RequestLevelEnum.fromValue(mediatedRequest.getRequestLevel().getValue()))
      .requestType(Request.RequestTypeEnum.fromValue(mediatedRequest.getRequestType().getValue()))
      .instanceId(asString(mediatedRequest.getInstanceId()))
      .holdingsRecordId(asString(mediatedRequest.getHoldingsRecordId()))
      .itemId(asString(mediatedRequest.getItemId()))
      .requesterId(asString(mediatedRequest.getRequesterId()))
      .fulfillmentPreference(Request.FulfillmentPreferenceEnum.HOLD_SHELF)
      .pickupServicePointId(pickupServicePointId)
      .requestDate(mediatedRequest.getRequestDate())
      .deliveryAddressTypeId(null)
      .patronComments(mediatedRequest.getPatronComments());
  }

  @Override
  public Request update(Request request) {
    log.info("update:: update circulation request {}", request.getId());
    return circulationClient.updateRequest(request.getId(), request);
  }
}
