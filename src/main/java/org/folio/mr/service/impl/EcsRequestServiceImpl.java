package org.folio.mr.service.impl;

import static org.folio.mr.support.ConversionUtils.asString;

import org.folio.mr.client.EcsTlrClient;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.EcsRequestService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class EcsRequestServiceImpl implements EcsRequestService {

  private final EcsTlrClient ecsTlrClient;
  private final SystemUserScopedExecutionService executionService;
  private final ConsortiumService consortiumService;

  @Override
  public EcsTlr create(MediatedRequestEntity mediatedRequest) {
    return executionService.executeSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> ecsTlrClient.post(buildEcsTlr(mediatedRequest)));
  }

//  @Override
//  public EcsTlr create(MediatedRequestEntity mediatedRequest, String centralTenantId) {
//    log.info("create:: creating ECS TLR for mediated request {}", mediatedRequest::getId);
//
//    return executionService.executeSystemUserScoped(centralTenantId,
//      () -> ecsTlrClient.post(buildEcsTlr(mediatedRequest)));
//  }

  private static EcsTlr buildEcsTlr(MediatedRequestEntity mediatedRequest) {
    return new EcsTlr()
      .requestType(EcsTlr.RequestTypeEnum.fromValue(mediatedRequest.getRequestType().getValue()))
      .requestLevel(EcsTlr.RequestLevelEnum.fromValue(mediatedRequest.getRequestLevel().getValue()))
      .fulfillmentPreference(EcsTlr.FulfillmentPreferenceEnum.fromValue(mediatedRequest.getFulfillmentPreference().getValue()))
      .instanceId(asString(mediatedRequest.getInstanceId()))
      .requesterId(asString(mediatedRequest.getRequesterId()))
      .pickupServicePointId(asString(mediatedRequest.getPickupServicePointId()))
//      .deliveryAddressTypeId(asString(mediatedRequest.getDeliveryAddressTypeId()))
      .requestDate(mediatedRequest.getRequestDate())
      .patronComments(mediatedRequest.getPatronComments());
  }
}
