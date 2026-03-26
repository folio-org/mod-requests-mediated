package org.folio.mr.config;

import java.util.Optional;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableResilientMethods
public class JpaConfig {

  @Bean
  public AuditorAware<UUID> auditorProvider(FolioExecutionContext folioExecutionContext) {
    return () -> Optional.ofNullable(folioExecutionContext.getUserId());
  }
}
