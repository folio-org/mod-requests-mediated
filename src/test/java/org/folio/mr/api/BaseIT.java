package org.folio.mr.api;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON;

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
import org.folio.mr.util.DbInitializer;
import org.folio.mr.util.MockHelper;
import org.folio.mr.util.TestUtils;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.extensions.EnablePostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
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
public class BaseIT {
  protected static final String TOKEN = "test_token";
  protected static final String TENANT_ID_CONSORTIUM = "consortium";
  protected static final String TENANT_ID_COLLEGE = "college";
  protected static final String TENANT_ID_SECURE = "secure";
  protected static final String TENANT_ID_CENTRAL = "central";
  protected static final String USER_ID = randomId();
  protected static final String HEADER_TENANT = "x-okapi-tenant";
  private static final String FOLIO_ENVIRONMENT = "folio";
  private static final int WIRE_MOCK_PORT = TestSocketUtils.findAvailableTcpPort();
  protected static WireMockServer wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
  protected static final String REQUEST_KAFKA_TOPIC_NAME = buildTopicName("circulation", "request");
  private static final String[] KAFKA_TOPICS = {REQUEST_KAFKA_TOPIC_NAME};
  private FolioExecutionContextSetter contextSetter;
  protected static MockHelper mockHelper;
  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  @Autowired
  private WebTestClient webClient;

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  private FolioModuleMetadata moduleMetadata;
  protected static AdminClient kafkaAdminClient;
  @Autowired
  protected KafkaTemplate<String, String> kafkaTemplate;

  @Container
  private static final KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.5.3"));

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    registry.add("folio.okapi-url", wireMockServer::baseUrl);
    registry.add("folio.tenant.secure-tenant-id", () -> TENANT_ID_CONSORTIUM);
  }

  @BeforeEach
  void beforeEachTest() {
    doPost("/_/tenant", asJsonString(new TenantAttributes().moduleTo("mod-requests-mediated")))
      .expectStatus().isNoContent();

    contextSetter = initFolioContext();
    wireMockServer.resetAll();
  }

  @AfterEach
  public void afterEachTest() {
    contextSetter.close();
  }

  @BeforeAll
  static void setUp() {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();
    mockHelper = new MockHelper(wireMockServer);

    kafkaAdminClient = KafkaAdminClient.create(Map.of(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
    createKafkaTopics(KAFKA_TOPICS);
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
    HashMap<String, Collection<String>> headers = new HashMap<>(defaultHeaders().entrySet()
      .stream()
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    return new FolioExecutionContextSetter(moduleMetadata, headers);
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

  protected static String buildTopicName(String module, String objectType) {
    return buildTopicName(FOLIO_ENVIRONMENT, TENANT_ID_CONSORTIUM, module, objectType);
  }

  private static String buildTopicName(String env, String tenant, String module, String objectType) {
    return String.format("%s.%s.%s.%s", env, tenant, module, objectType);
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }

}
