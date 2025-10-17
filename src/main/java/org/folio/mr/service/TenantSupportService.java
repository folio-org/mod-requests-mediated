package org.folio.mr.service;

public interface TenantSupportService {

  boolean isCentralTenant(String tenantId);

  boolean isSecureTenant(String tenantId);
}
