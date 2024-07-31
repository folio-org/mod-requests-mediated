package org.folio.mr.util;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;

public class DbInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  protected static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:12-alpine");

  static {
    postgreDBContainer.start();
  }

  @Override
  public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext,
      "spring.datasource.url=" + postgreDBContainer.getJdbcUrl(),
      "spring.datasource.username=" + postgreDBContainer.getUsername(),
      "spring.datasource.password=" + postgreDBContainer.getPassword());
  }

}
