package org.folio.mr.service.impl;

import static org.folio.mr.support.ConversionUtils.asString;

import org.folio.mr.client.EcsTlrClient;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.entity.FakeUser;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.FakeUserRepository;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.EcsRequestService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class EcsRequestServiceImpl implements EcsRequestService {

  private static final String STAFF_SERVICE_POINT_ID = "c4c90014-c8c9-4ade-8f24-b5e313319f4b";

  private final FakeUserRepository fakeUserRepository;
  private final EcsTlrClient ecsTlrClient;
  private final SystemUserScopedExecutionService executionService;
  private final ConsortiumService consortiumService;

  @Override
  public EcsTlr create(MediatedRequestEntity mediatedRequest) {
    log.info("confirm:: creating ECS TLR for mediated request {}", mediatedRequest::getId);
    return executionService.executeSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> ecsTlrClient.post(buildEcsTlr(mediatedRequest)));
  }

  private EcsTlr buildEcsTlr(MediatedRequestEntity mediatedRequest) {
    FakeUser fakeUser = saveFakeUser(mediatedRequest.getRequesterId());
    return new EcsTlr()
      .requestType(EcsTlr.RequestTypeEnum.fromValue(mediatedRequest.getRequestType().getValue()))
      .requestLevel(EcsTlr.RequestLevelEnum.fromValue(mediatedRequest.getRequestLevel().getValue()))
      .fulfillmentPreference(EcsTlr.FulfillmentPreferenceEnum.fromValue(
        mediatedRequest.getFulfillmentPreference().getValue()))
      .instanceId(asString(mediatedRequest.getInstanceId()))
      .requesterId(fakeUser.getId().toString())
      .pickupServicePointId(STAFF_SERVICE_POINT_ID)
      .requestDate(mediatedRequest.getRequestDate())
      .patronComments(mediatedRequest.getPatronComments());
  }

  private FakeUser saveFakeUser(UUID userId) {
    FakeUser fakeUser = new FakeUser();
    fakeUser.setUserId(userId);
    fakeUserRepository.save(fakeUser);
    return fakeUser;
  }
}
