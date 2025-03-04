package org.folio.mr.service.impl;

import static java.lang.String.format;
import static org.folio.mr.support.Constants.INTERIM_SERVICE_POINT_ID;
import static org.folio.mr.support.ConversionUtils.asString;

import java.util.UUID;

import org.folio.mr.client.EcsTlrClient;
import org.folio.mr.domain.dto.EcsTlr;
import org.folio.mr.domain.dto.User;
import org.folio.mr.domain.dto.UserPersonal;
import org.folio.mr.domain.entity.FakePatronLink;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.repository.FakePatronLinkRepository;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.EcsRequestService;
import org.folio.mr.service.UserService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class EcsRequestServiceImpl implements EcsRequestService {
  private final FakePatronLinkRepository fakePatronLinkRepository;
  private final EcsTlrClient ecsTlrClient;
  private final UserService userService;
  private final SystemUserScopedExecutionService executionService;
  private final ConsortiumService consortiumService;

  @Override
  public EcsTlr create(MediatedRequestEntity mediatedRequest) {
    log.info("create:: Creating fake patron for mediated request {}", mediatedRequest.getId());
    String fakeUserId = createFakePatron(mediatedRequest);

    log.info("create:: Creating fake patron link between real patron {} and fake patron {}",
      mediatedRequest.getRequesterId(), fakeUserId);
    createFakePatronLink(mediatedRequest.getRequesterId(), fakeUserId);

    log.info("create:: Creating ECS request for fake patron {}, mediated request {}", fakeUserId,
      mediatedRequest.getId());
    return createEcsTlr(mediatedRequest, fakeUserId, INTERIM_SERVICE_POINT_ID);
  }

  private String createFakePatron(MediatedRequestEntity mediatedRequest) {
    User realPatron = userService.fetchUser(asString(mediatedRequest.getRequesterId()));
    User fakePatron = buildFakePatron(realPatron.getPatronGroup());

    log.info("createFakePatron:: Creating local fake patron");
    User localFake = userService.create(fakePatron);

    return localFake.getId();
  }

  private User buildFakePatron(String patronGroupId) {
    String randomSecurePatronStr = format("securepatron_%s", UUID.randomUUID());
    return new User()
      .active(true)
      .barcode(randomSecurePatronStr)
      .type("patron")
      .patronGroup(patronGroupId)
      .personal(
        new UserPersonal()
          .preferredContactTypeId("002")
          .firstName("Secure")
          .lastName("Patron")
          .email(randomSecurePatronStr)
      );
  }

  private FakePatronLink createFakePatronLink(UUID userId, String fakeUserId) {
    FakePatronLink fakePatronLink = new FakePatronLink();
    fakePatronLink.setUserId(userId);
    fakePatronLink.setFakeUserId(UUID.fromString(fakeUserId));
    return fakePatronLinkRepository.save(fakePatronLink);
  }

  private EcsTlr createEcsTlr(MediatedRequestEntity mediatedRequest, String requesterId,
    String pickupServicePointId) {

    EcsTlr ecsTlr = new EcsTlr()
      .primaryRequestTenantId(consortiumService.getCurrentTenantId())
      .requestType(EcsTlr.RequestTypeEnum.fromValue(mediatedRequest.getRequestType().getValue()))
      .requestLevel(EcsTlr.RequestLevelEnum.fromValue(mediatedRequest.getRequestLevel().getValue()))
      .instanceId(asString(mediatedRequest.getInstanceId()))
      .itemId(asString(mediatedRequest.getItemId()))
      .holdingsRecordId(asString(mediatedRequest.getHoldingsRecordId()))
      .requesterId(requesterId)
      .fulfillmentPreference(EcsTlr.FulfillmentPreferenceEnum.HOLD_SHELF)
      .pickupServicePointId(pickupServicePointId)
      .requestDate(mediatedRequest.getRequestDate())
      .patronComments(mediatedRequest.getPatronComments());
    return executionService.executeSystemUserScoped(consortiumService.getCentralTenantId(),
      () -> ecsTlrClient.post(ecsTlr));
  }
}
