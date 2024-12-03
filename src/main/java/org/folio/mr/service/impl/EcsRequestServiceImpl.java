package org.folio.mr.service.impl;

import static org.folio.mr.support.ConversionUtils.asString;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.mr.client.EcsTlrClient;
import org.folio.mr.client.UserClient;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserPersonal;
import org.folio.mr.domain.entity.FakePatronLink;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.FakePatronLinkRepository;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.EcsRequestService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class EcsRequestServiceImpl implements EcsRequestService {

  private final FakePatronLinkRepository fakePatronLinkRepository;
  private final EcsTlrClient ecsTlrClient;
  private final UserClient userClient;
  private final SystemUserScopedExecutionService executionService;
  private final ConsortiumService consortiumService;

  @Override
  public EcsTlr create(MediatedRequestEntity mediatedRequest) {
    log.info("create:: creating fake user");
    User fakeUser = createFakeUser();
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    log.info("create:: creating fake patron link");
    FakePatronLink fakePatronLink = createFakePatronLink(mediatedRequest.getRequesterId(), fakeUser.getId());

    log.info("create:: creating ECS TLR for mediated request {}", mediatedRequest.getId());
    return createEcsTlr(mediatedRequest, fakePatronLink.getFakeUserId());
  }

  private User createFakeUser() {
    User user = new User()
      .active(true)
      .type("patron")
      .patronGroup("bdc2b6d4-5ceb-4a12-ab46-249b9a68473e")
      .personal(
        new UserPersonal()
          .preferredContactTypeId("002")
          .firstName("Secure")
          .lastName("Patron")
          .email(UUID.randomUUID() + "@example.com")
      );
    return executionService.executeSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> userClient.postUser(user));
  }

  private FakePatronLink createFakePatronLink(UUID userId, String fakeUserId) {
    FakePatronLink fakePatronLink = new FakePatronLink();
    fakePatronLink.setUserId(userId);
    fakePatronLink.setFakeUserId(UUID.fromString(fakeUserId));
    return fakePatronLinkRepository.save(fakePatronLink);
  }

  private EcsTlr createEcsTlr(MediatedRequestEntity mediatedRequest, UUID requesterId) {
    EcsTlr ecsTlr = new EcsTlr()
      .primaryRequestTenantId(consortiumService.getCurrentTenantId())
      .requestType(EcsTlr.RequestTypeEnum.fromValue(mediatedRequest.getRequestType().getValue()))
      .requestLevel(EcsTlr.RequestLevelEnum.fromValue(mediatedRequest.getRequestLevel().getValue()))
      .fulfillmentPreference(EcsTlr.FulfillmentPreferenceEnum.fromValue(
        mediatedRequest.getFulfillmentPreference().getValue()))
      .instanceId(asString(mediatedRequest.getInstanceId()))
      .itemId(asString(mediatedRequest.getItemId()))
      .holdingsRecordId(asString(mediatedRequest.getHoldingsRecordId()))
      .requesterId(asString(requesterId))
      .pickupServicePointId(asString(mediatedRequest.getPickupServicePointId()))
      .requestDate(mediatedRequest.getRequestDate())
      .patronComments(mediatedRequest.getPatronComments());
    return executionService.executeSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> ecsTlrClient.post(ecsTlr));
  }
}
