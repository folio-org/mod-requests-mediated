package org.folio.mr.service;

import java.util.Optional;

public interface TenantSupportService {

  Optional<String> getCentralTenantId();

  boolean isCentralTenant(String tenantId);

  boolean isSecureTenant(String tenantId);
}
