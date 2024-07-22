package org.folio.mr.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import org.folio.mr.util.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = BaseIT.DockerPostgresDataSourceInitializer.class)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext
@Log4j2
public class BaseIT {
  protected static final String HEADER_TENANT = "x-okapi-tenant";
  protected static final String TOKEN = "test_token";
  public static final String TENANT_ID_DIKU = "diku";
  protected static final String TENANT_ID_CONSORTIUM = "consortium";
  protected static final String TENANT_ID_UNIVERSITY = "university";
  protected static final String TENANT_ID_COLLEGE = "college";
  @Autowired
  private WebTestClient webClient;
  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  private FolioExecutionContext context;
  @Autowired
  private FolioModuleMetadata moduleMetadata;
  public static WireMockServer wireMockServer;
  private FolioExecutionContextSetter contextSetter;
  protected static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:12-alpine");
  public static final int WIRE_MOCK_PORT = TestSocketUtils.findAvailableTcpPort();
  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  static {
    postgreDBContainer.start();
  }

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();
    setUpTenant(mockMvc);
  }

  @BeforeEach
  void beforeEachTest() {
    contextSetter = initFolioContext();
  }

  @AfterEach
  public void afterEachTest() {
    contextSetter.close();
  }

  protected FolioExecutionContextSetter initFolioContext() {
    return new FolioExecutionContextSetter(moduleMetadata, buildDefaultHeaders());
  }

  public static String getOkapiUrl() {
    return String.format("http://localhost:%s", WIRE_MOCK_PORT);
  }

  @AfterAll
  static void tearDown() {
    wireMockServer.stop();
  }

  @SneakyThrows
  protected static void setUpTenant(MockMvc mockMvc) {
    mockMvc.perform(post("/_/tenant")
      .content(asJsonString(new TenantAttributes().moduleTo("mod-requests-mediated")))
      .headers(defaultHeaders())
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT_ID_CONSORTIUM));
    httpHeaders.add(XOkapiHeaders.URL, wireMockServer.baseUrl());
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, "08d51c7a-0f36-4f3d-9e35-d285612a23df");

    return httpHeaders;
  }

  private static Map<String, Collection<String>> buildDefaultHeaders() {
    return new HashMap<>(defaultHeaders().entrySet()
      .stream()
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  public static class DockerPostgresDataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
        "spring.datasource.url=" + postgreDBContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgreDBContainer.getUsername(),
        "spring.datasource.password=" + postgreDBContainer.getPassword());
    }
  }

  protected WebTestClient.RequestBodySpec buildRequest(HttpMethod method, String uri) {
    return webClient.method(method)
      .uri(uri)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID_CONSORTIUM)
      .header(XOkapiHeaders.URL, wireMockServer.baseUrl())
      .header(XOkapiHeaders.TOKEN, TOKEN)
      .header(XOkapiHeaders.USER_ID, randomId());
  }

  protected WebTestClient.ResponseSpec doGet(String url) {
    return buildRequest(HttpMethod.GET, url)
      .exchange();
  }

  protected WebTestClient.ResponseSpec doPost(String url, Object payload) {
    return doPostWithTenant(url, payload, TENANT_ID_CONSORTIUM);
  }

  protected WebTestClient.ResponseSpec doPostWithTenant(String url, Object payload, String tenantId) {
    return doPostWithToken(url, payload, TestUtils.buildToken(tenantId));
  }

  protected WebTestClient.ResponseSpec doPostWithToken(String url, Object payload, String token) {
    return buildRequest(HttpMethod.POST, url)
      .cookie("folioAccessToken", token)
      .body(BodyInserters.fromValue(payload))
      .exchange();
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }

}
