package org.folio.mr.service.impl;

import java.util.Locale;
import java.util.Optional;

import org.folio.mr.client.ServicePointClient;
import org.folio.mr.domain.dto.ServicePoint;
import org.folio.mr.domain.dto.TimePeriod;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.InterimServicePointService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class InterimServicePointServiceImpl implements InterimServicePointService {

  public static final String INTERIM_SERVICE_POINT_ID = "32c6f0c7-26e4-4350-8c29-1e11c2e3efc4";

  private final ServicePointClient servicePointClient;
  private final ConsortiumService consortiumService;
  private final SystemUserScopedExecutionService executionService;
  private final FolioExecutionContext context;

  @Override
  public void createInterimServicePoint() {
    var currentTenantId = context.getTenantId();
    var centralTenantId = executionService.executeSystemUserScoped(currentTenantId,
      () -> consortiumService.getCentralTenantId(currentTenantId)).orElse(currentTenantId);

    log.info("createInterimServicePoint:: creating Interim service point in tenant {}", centralTenantId);
    executionService.executeSystemUserScoped(centralTenantId, () -> {
      create();
      return null;
    });
  }

  private void create() {
    try {
      servicePointClient.create(buildInterimServicePoint());
      log.info("create:: Interim service point created");
    } catch (HttpStatusCodeException e) {
      if (isAlreadyExists(e)) {
        log.info("create:: Interim service point already exists, ignoring");
        return;
      }
      throw e;
    }
  }

  static boolean isAlreadyExists(HttpStatusCodeException e) {
    var status = e.getStatusCode().value();
    if (status != HttpStatus.UNPROCESSABLE_ENTITY.value() && status != HttpStatus.BAD_REQUEST.value()
      && status != HttpStatus.CONFLICT.value()) {
      return false;
    }
    var body = Optional.ofNullable(e.getResponseBodyAsString()).orElse("").toLowerCase(Locale.ROOT);
    return body.contains("already exists") || body.contains("duplicate");
  }

  static ServicePoint buildInterimServicePoint() {
    return new ServicePoint()
      .id(INTERIM_SERVICE_POINT_ID)
      .name("Interim service point")
      .code("interimsp")
      .discoveryDisplayName("Interim service point")
      .pickupLocation(true)
      .holdShelfExpiryPeriod(new TimePeriod().duration(3).intervalId(TimePeriod.IntervalIdEnum.WEEKS));
  }
}
