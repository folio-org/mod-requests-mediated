package org.folio.mr.util;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public class WireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  protected static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:12-alpine");

  static {
    postgreDBContainer.start();
  }

  @Override
  public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
    WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
    wireMockServer.start();

    configurableApplicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

    configurableApplicationContext.addApplicationListener(applicationEvent -> {
      if (applicationEvent instanceof ContextClosedEvent) {
        wireMockServer.stop();
      }
    });

    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext,
      "spring.datasource.url=" + postgreDBContainer.getJdbcUrl(),
      "spring.datasource.username=" + postgreDBContainer.getUsername(),
      "spring.datasource.password=" + postgreDBContainer.getPassword());
  }
}
