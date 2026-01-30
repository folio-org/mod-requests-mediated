package org.folio.mr.service;

import java.util.Optional;

public interface ConsortiumService {
  String getCentralTenantId();

  String getCurrentTenantId();

  Optional<String> getCentralTenantId(String tenantId);
}
