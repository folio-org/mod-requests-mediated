package org.folio.mr.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@ConfigurationProperties("folio.tenant")
@Validated
public class TenantConfig {
  @NotNull
  private String secureTenantId;
}
