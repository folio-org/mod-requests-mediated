package org.folio.mr.service.impl;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CustomTenantService extends TenantService {

  private final PrepareSystemUserService systemUserService;

  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
    FolioSpringLiquibase folioSpringLiquibase, PrepareSystemUserService systemUserService) {

    super(jdbcTemplate, context, folioSpringLiquibase);
    this.systemUserService = systemUserService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.debug("afterTenantUpdate:: tenantAttributes={}", tenantAttributes);
    log.info("afterTenantUpdate:: setting up system user");
    systemUserService.setupSystemUser();
  }
}
