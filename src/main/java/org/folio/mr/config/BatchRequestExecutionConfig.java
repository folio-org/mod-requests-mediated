package org.folio.mr.config;

import java.util.concurrent.Executors;
import org.folio.flow.api.FlowEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchRequestExecutionConfig {

  @Bean
  public FlowEngine flowEngine(BatchRequestExecutionProperties configuration) {
    return FlowEngine.builder()
      .name("batch-request-flow-engine")
      .executor(Executors.newFixedThreadPool(configuration.getThreadPoolSize()))
      .executionTimeout(configuration.getExecutionTimeout())
      .printFlowResult(configuration.getPrintResult())
      .lastExecutionsStatusCacheSize(configuration.getLastExecutionsStatusCacheSize())
      .build();
  }
}
