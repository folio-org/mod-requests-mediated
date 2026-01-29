package org.folio.mr.api;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.mr.support.DatabaseHelper;
import org.folio.mr.util.DbInitializer;
import org.folio.mr.util.MockHelper;
import org.folio.mr.util.TestUtils;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.testing.extension.impl.OkapiConfiguration;
import org.folio.spring.testing.extension.impl.OkapiExtension;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.extensions.EnablePostgres;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;

import lombok.SneakyThrows;

@EnablePostgres
@ActiveProfiles("test")
@ContextConfiguration(initializers = {DbInitializer.class})
@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
  "spring.kafka.consumer.auto-offset-reset=earliest"
})
@AutoConfigureMockMvc
@Testcontainers
@Import(BaseIT.DbHelperTestConfiguration.class)
public class BaseIT {
  protected static final String TOKEN = "test_token";
  protected static final String TENANT_ID_CONSORTIUM = "consortium";
  protected static final String TENANT_ID_COLLEGE = "college";
  protected static final String TENANT_ID_SECURE = "secure";
  protected static final String TENANT_ID_CENTRAL = "central";
  protected static final String USER_ID = randomId();
  protected static final String HEADER_TENANT = "x-okapi-tenant";
  private static final String FOLIO_ENVIRONMENT = "folio";
  protected static final String REQUEST_KAFKA_TOPIC_NAME = buildTopicName("circulation", "request");
  protected static final String ITEM_KAFKA_TOPIC_NAME = buildTopicName("inventory", "item");
  private static final String[] KAFKA_TOPICS = {REQUEST_KAFKA_TOPIC_NAME, ITEM_KAFKA_TOPIC_NAME};
  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  protected static WebTestClient webClient;

  protected static OkapiConfiguration okapi;

  protected static AdminClient kafkaAdminClient;

  protected static WireMockServer wireMockServer;

  protected static MockHelper mockHelper;

  protected static DatabaseHelper databaseHelper;

  protected FolioExecutionContextSetter contextSetter;

  @RegisterExtension
  static OkapiExtension okapiExtension = new OkapiExtension();

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  private FolioModuleMetadata moduleMetadata;

  @Autowired
  protected KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @Container
  private static final KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("apache/kafka-native:3.8.0"))
    .withStartupAttempts(3);

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    registry.add("folio.okapi-url", okapi::getOkapiUrl);
  }

  @SneakyThrows
  protected static void setUpTenant() {
    setUpTenant(TENANT_ID_CONSORTIUM);
  }

  @SneakyThrows
  protected static void setUpTenant(String tenantId) {
    var httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.add(XOkapiHeaders.TENANT, tenantId);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);
    httpHeaders.add(XOkapiHeaders.URL, okapi.getOkapiUrl());

    var tenantAttributes = new TenantAttributes().moduleTo("mod-requests-mediated");
    doPostWithTenant("/_/tenant", tenantAttributes, tenantId, httpHeaders);
  }

  @BeforeEach
  void beforeEachTest() {
    contextSetter = initFolioContext();
    wireMockServer.resetAll();
  }

  @AfterEach
  void afterEachTest() {
    contextSetter.close();
  }

  @BeforeAll
  static void setUp(@Autowired WebTestClient webClient, @Autowired DatabaseHelper databaseHelper) {
    BaseIT.webClient = webClient;

    wireMockServer = okapi.wireMockServer();
    mockHelper = new MockHelper(wireMockServer);
    BaseIT.databaseHelper = databaseHelper;

    kafkaAdminClient = KafkaAdminClient.create(Map.of(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
    createKafkaTopics(KAFKA_TOPICS);

    setUpTenant();
  }

  @AfterAll
  static void tearDown() {
    kafkaAdminClient.close();
    wireMockServer.stop();
  }

  @SneakyThrows
  private static void createKafkaTopics(String... topicNames) {
    List<NewTopic> topics = Arrays.stream(topicNames)
      .map(topic -> new NewTopic(topic, 1, (short) 1))
      .toList();

    kafkaAdminClient.createTopics(topics)
      .all()
      .get(10, TimeUnit.SECONDS);
  }

  protected FolioExecutionContextSetter initFolioContext() {
    return initFolioContext(TENANT_ID_CONSORTIUM);
  }

  protected FolioExecutionContextSetter initFolioContext(String tenantId) {
    var headers = defaultHeaders();
    headers.set(XOkapiHeaders.TENANT, tenantId);
    HashMap<String, Collection<String>> headersMap = new HashMap<>(headers.entrySet()
      .stream()
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    return new FolioExecutionContextSetter(moduleMetadata, headersMap);
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.add(XOkapiHeaders.TENANT, TENANT_ID_CONSORTIUM);
    httpHeaders.add(XOkapiHeaders.URL, (wireMockServer.baseUrl()));
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);

    return httpHeaders;
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  protected static WebTestClient.RequestBodySpec buildRequest(HttpMethod method, String uri, HttpHeaders headers) {
    return webClient.method(method)
      .uri(uri)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, headers.getFirst(XOkapiHeaders.TENANT))
      .header(XOkapiHeaders.URL, okapi.getOkapiUrl())
      .header(XOkapiHeaders.TOKEN, TOKEN)
      .header(XOkapiHeaders.USER_ID, headers.getFirst(XOkapiHeaders.USER_ID));
  }

  protected WebTestClient.ResponseSpec doPost(String url, Object payload) {
    return doPostWithTenant(url, payload, TENANT_ID_CONSORTIUM);
  }

  protected WebTestClient.ResponseSpec doPostWithTenant(String url, Object payload, String tenantId) {
    return doPostWithToken(url, payload, TestUtils.buildToken(tenantId), defaultHeaders());
  }

  protected static WebTestClient.ResponseSpec doPostWithTenant(String url, Object payload, String tenantId, HttpHeaders headers) {
    return doPostWithToken(url, payload, TestUtils.buildToken(tenantId), headers);
  }

  protected static WebTestClient.ResponseSpec doPostWithToken(String url, Object payload, String token, HttpHeaders headers) {
    return buildRequest(HttpMethod.POST, url, headers)
      .cookie("folioAccessToken", token)
      .body(BodyInserters.fromValue(payload))
      .exchange();
  }

  protected static String buildTopicName(String module, String objectType) {
    return buildTopicName(FOLIO_ENVIRONMENT, TENANT_ID_CONSORTIUM, module, objectType);
  }

  private static String buildTopicName(String env, String tenant, String module, String objectType) {
    return String.format("%s.%s.%s.%s", env, tenant, module, objectType);
  }

  protected static Collection<Header> buildHeadersForKafkaProducer(String tenant) {
    return buildKafkaHeaders(tenant)
      .entrySet()
      .stream()
      .map(entry -> new RecordHeader(entry.getKey(), (byte[]) entry.getValue()))
      .collect(toList());
  }

  protected static Map<String, Object> buildKafkaHeaders(String tenantId) {
    Map<String, String> headers = buildHeaders(tenantId);
    headers.put("folio.tenantId", tenantId);

    return headers.entrySet()
      .stream()
      .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().getBytes()));
  }

  protected static Map<String, String> buildHeaders(String tenantId) {
    Map<String, String> headers = new HashMap<>();
    headers.put(XOkapiHeaders.TENANT, tenantId);
    headers.put(XOkapiHeaders.URL, wireMockServer.baseUrl());
    headers.put(XOkapiHeaders.TOKEN, TOKEN);
    headers.put(XOkapiHeaders.USER_ID, USER_ID);
    headers.put(XOkapiHeaders.REQUEST_ID, randomId());
    return headers;
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }

  protected <T> ResultMatcher exceptionMatch(Class<T> type) {
    return result -> MatcherAssert.assertThat(result.getResolvedException(), instanceOf(type));
  }

  protected ResultMatcher errorMessageMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].message", errorMessageMatcher);
  }

  protected ResultMatcher errorTypeMatch(Matcher<String> errorMessageMatcher) {
    return jsonPath("$.errors.[0].type", errorMessageMatcher);
  }

  protected ResultMatcher errorCodeMatch(Matcher<String> code) {
    return jsonPath("$.errors.[0].code", code);
  }

  @TestConfiguration
  public static class DbHelperTestConfiguration {

    @Bean
    public DatabaseHelper databaseHelper(JdbcTemplate jdbcTemplate, FolioModuleMetadata moduleMetadata) {
      return new DatabaseHelper(moduleMetadata, jdbcTemplate);
    }
  }
}
