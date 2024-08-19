package org.folio.mr.api;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.mr.util.DbInitializer;
import org.folio.mr.util.TestUtils;
import org.folio.mr.util.WireMockInitializer;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.extensions.EnablePostgres;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.BodyInserters;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;

import lombok.SneakyThrows;

@EnablePostgres
@ActiveProfiles("test")
@ContextConfiguration(initializers = {WireMockInitializer.class, DbInitializer.class})
@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class BaseIT {
  protected static final String TOKEN = "test_token";
  protected static final String TENANT_ID_CONSORTIUM = "consortium";
  protected static final String USER_ID = randomId();

  @Autowired
  private WebTestClient webClient;
  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  private FolioModuleMetadata moduleMetadata;
  @Autowired
  public WireMockServer wireMockServer;
  private FolioExecutionContextSetter contextSetter;

  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    setUpTenant(mockMvc);
  }

  @BeforeEach
  void beforeEachTest() {
    contextSetter = initFolioContext();
    wireMockServer.resetAll();
  }

  @AfterEach
  public void afterEachTest() {
    contextSetter.close();
  }

  protected FolioExecutionContextSetter initFolioContext() {
    HashMap<String, Collection<String>> headers = new HashMap<>(buildHeaders().entrySet()
      .stream()
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    return new FolioExecutionContextSetter(moduleMetadata, headers);
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
    httpHeaders.add(XOkapiHeaders.TENANT, TENANT_ID_CONSORTIUM);
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);

    return httpHeaders;
  }

  protected HttpHeaders buildHeaders() {
    HttpHeaders httpHeaders = defaultHeaders();
    httpHeaders.add(XOkapiHeaders.URL, (wireMockServer.baseUrl()));
    return httpHeaders;
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  protected WebTestClient.RequestBodySpec buildRequest(HttpMethod method, String uri) {
    return webClient.method(method)
      .uri(uri)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID_CONSORTIUM)
      .header(XOkapiHeaders.URL, wireMockServer.baseUrl())
      .header(XOkapiHeaders.TOKEN, TOKEN)
      .header(XOkapiHeaders.USER_ID, USER_ID);
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
