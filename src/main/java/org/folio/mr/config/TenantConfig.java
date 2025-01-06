package org.folio.mr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@ConfigurationProperties("folio.tenant.secure-tenant-id")
@Validated
public class TenantConfig {
  private String secureTenantId;
}
