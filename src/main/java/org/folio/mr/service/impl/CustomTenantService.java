package org.folio.mr.service.impl;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@Primary
public class CustomTenantService extends TenantService {

  private final PrepareSystemUserService prepareSystemUserService;

  public CustomTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context,
    FolioSpringLiquibase folioSpringLiquibase, PrepareSystemUserService prepareSystemUserService) {

    super(jdbcTemplate, context, folioSpringLiquibase);
    this.prepareSystemUserService = prepareSystemUserService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.info("afterTenantUpdate:: tenantAttributes={}", tenantAttributes);
    prepareSystemUserService.setupSystemUser();
  }
}
