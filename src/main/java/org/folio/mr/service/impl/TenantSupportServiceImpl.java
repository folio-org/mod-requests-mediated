package org.folio.mr.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.mr.config.TenantConfig;
import org.folio.mr.service.ConsortiumService;
import org.folio.mr.service.TenantSupportService;
import org.springframework.stereotype.Service;


@Log4j2
@Service
@RequiredArgsConstructor
public class TenantSupportServiceImpl implements TenantSupportService {

  private final ConsortiumService consortiumService;
  private final TenantConfig tenantConfig;

  @Override
  public boolean isCentralTenant(String tenantId) {
    return consortiumService.getCentralTenantId(tenantId)
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
