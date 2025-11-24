package org.folio.mr.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.mr.client.UserTenantsClient;
import org.folio.mr.config.TenantConfig;
import org.folio.mr.domain.dto.UserTenant;
import org.folio.mr.service.TenantSupportService;
import org.springframework.stereotype.Service;


@Log4j2
@Service
@RequiredArgsConstructor
public class TenantSupportServiceImpl implements TenantSupportService {

  private final UserTenantsClient userTenantsClient;
  private final TenantConfig tenantConfig;

  @Override
  public Optional<String> getCentralTenantId() {
    return Optional.ofNullable(userTenantsClient.getUserTenants(1))
      .flatMap(tenantsResponse -> tenantsResponse.getUserTenants().stream()
        .findFirst()
        .map(UserTenant::getCentralTenantId));
  }

  @Override
  public boolean isCentralTenant(String tenantId) {
    return getCentralTenantId()
      .filter(centralTenantId -> centralTenantId.equals(tenantId))
      .isPresent();
  }

  @Override
  public boolean isSecureTenant(String tenantId) {
    var secureTenantId = tenantConfig.getSecureTenantId();
    if (StringUtils.isBlank(secureTenantId)) {
      log.warn("isSecureTenant:: secure tenant ID is not provided");
      return false;
    }

    return secureTenantId.equals(tenantId);
  }
}
