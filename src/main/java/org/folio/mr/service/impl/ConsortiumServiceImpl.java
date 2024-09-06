package org.folio.mr.service.impl;

import org.folio.mr.client.UserTenantsClient;
import org.folio.mr.domain.dto.UserTenant;
import org.folio.mr.service.ConsortiumService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ConsortiumServiceImpl implements ConsortiumService {

  private final UserTenantsClient userTenantsClient;

  @Override
  @Cacheable("centralTenant") // implement caching
  public String getCentralTenantId() {
    log.info("getCentralTenantId:: resolving central tenant ID");
    String centralTenantId = userTenantsClient.getUserTenants(1)
      .getUserTenants()
      .stream()
      .findFirst()
      .map(UserTenant::getCentralTenantId)
      .orElseThrow();

    log.info("getCentralTenantId:: central tenant ID: {}", centralTenantId);
    return centralTenantId;
  }
}
