package org.folio.mr.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.folio.mr.domain.dto.Request.StatusEnum.CLOSED_CANCELLED;
import static org.folio.mr.domain.dto.Request.StatusEnum.CLOSED_FILLED;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_AWAITING_PICKUP;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_IN_TRANSIT;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_NOT_YET_FILLED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.folio.mr.util.TestUtils.buildEvent;
import static org.folio.mr.util.TestUtils.buildInventoryEvent;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.awaitility.Awaitility;
import org.folio.mr.api.BaseIT;
import org.folio.mr.domain.dto.ConsortiumItem;
import org.folio.mr.domain.dto.Item;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.MediatedRequestItem;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.RequestInstance;
import org.folio.mr.domain.dto.RequestItem;
import org.folio.mr.domain.dto.RequestRequester;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.mapper.MediatedRequestMapperImpl;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.support.kafka.DefaultKafkaEvent;
import org.folio.mr.support.kafka.InventoryKafkaEvent;
import org.folio.mr.support.kafka.KafkaEvent;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.SneakyThrows;

class KafkaEventListenerTest extends BaseIT {
  private static final String CONSUMER_GROUP_ID = "folio-mod-requests-mediated-group";
  private static final UUID CONFIRMED_REQUEST_ID = randomUUID();
  private static final String ITEM_ID = "9428231b-dd31-4f70-8406-fe22fbdeabc2";
  private static final String HOLDINGS_RECORD_ID = randomId();
  private static final String ITEM_BARCODE = "A14837334314";

  private static final String ITEM_STORAGE_URL = "/item-storage/items";

  private final MediatedRequestMapperImpl mediatedRequestMapper = new MediatedRequestMapperImpl();
  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;
  @Autowired
  private SystemUserScopedExecutionService executionService;

  @BeforeEach
  void beforeEach() {
    mediatedRequestsRepository.deleteAll();
  }

  @Test
  void mediatedRequestInStatusOpenNotYetFilledIsUpdatedUponConfirmedRequestUpdate() {
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_NOT_YET_FILLED, OPEN_IN_TRANSIT);
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL.getValue(),
      updatedMediatedRequest.getStatus());
  }

  @Test
  void localMediatedRequestInStatusOpenNotYetFilledIsUpdatedUponConfirmedRequestUpdate() {
    KafkaEvent<Request> event = buildLocalRequestUpdateEvent(OPEN_NOT_YET_FILLED, OPEN_IN_TRANSIT);
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest = createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(
      mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_FOR_APPROVAL.getValue(),
      updatedMediatedRequest.getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = Request.StatusEnum.class, mode = EXCLUDE,
    names = {"OPEN_IN_TRANSIT", "CLOSED_CANCELLED"})
  void mediatedRequestInStatusOpenNotYetFilledIsNotUpdatedUponConfirmedRequestUpdate(
    Request.StatusEnum newRequestStatus) {

    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_NOT_YET_FILLED, newRequestStatus);
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED.getValue(),
      updatedMediatedRequest.getStatus());
  }

  @ParameterizedTest
  @CsvSource({"Open - Awaiting pickup,Open - Awaiting pickup",
    "Open - Awaiting delivery,Open - Awaiting delivery"})
  void mediatedRequestInStatusOpenInTransitToBeCheckedOutIsUpdatedUponConfirmedRequestUpdate(
    String newRequestStatusStr, String newMediatedRequestStatusStr) {
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_IN_TRANSIT,
      Request.StatusEnum.fromValue(newRequestStatusStr));
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(newMediatedRequestStatusStr, updatedMediatedRequest.getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = Request.StatusEnum.class, mode = EXCLUDE,
    names = {"OPEN_AWAITING_PICKUP", "OPEN_AWAITING_DELIVERY", "CLOSED_CANCELLED"})
  void mediatedRequestInStatusOpenInTransitToBeCheckedOutIsNotUpdatedUponConfirmedRequestUpdate(
    Request.StatusEnum newRequestStatus) {

    KafkaEvent<Request> event = buildRequestUpdateEvent(newRequestStatus, OPEN_NOT_YET_FILLED );
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED.getValue(),
      updatedMediatedRequest.getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = Request.StatusEnum.class,
    names = {"OPEN_AWAITING_PICKUP", "OPEN_AWAITING_DELIVERY"})
  void mediatedRequestInStatusOpenAwaitingPickupIsUpdatedUponConfirmedRequestUpdate(
    Request.StatusEnum oldRequestStatus) {

    KafkaEvent<Request> event = buildRequestUpdateEvent(oldRequestStatus, CLOSED_FILLED);
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.CLOSED_FILLED.getValue(),
      updatedMediatedRequest.getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = Request.StatusEnum.class, mode = EXCLUDE,
    names = {"OPEN_AWAITING_PICKUP", "OPEN_AWAITING_DELIVERY", "CLOSED_CANCELLED"})
  void mediatedRequestInStatusOpenAwaitingPickupIsNotUpdatedUponConfirmedRequestUpdate(
    Request.StatusEnum oldRequestStatus) {

    KafkaEvent<Request> event = buildRequestUpdateEvent(oldRequestStatus, CLOSED_FILLED);
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP.getValue(),
      updatedMediatedRequest.getStatus());
  }

  @Test
  void shouldCancelMediatedRequestUponConfirmedRequestCancel() {
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_NOT_YET_FILLED, CLOSED_CANCELLED);
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.CLOSED_CANCELLED.getValue(), updatedMediatedRequest.getStatus());
  }

  @Test
  void shouldUpdateMediatedRequestStatusOnItemCheckout() {
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_IN_TRANSIT, OPEN_AWAITING_PICKUP);
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP.getValue(), updatedMediatedRequest.getStatus());
  }

  @Test
  void shouldCloseMediatedRequestUponConfirmedRequestFill() {
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_AWAITING_PICKUP, CLOSED_FILLED);
    var mediatedRequest = buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.CLOSED_FILLED.getValue(), updatedMediatedRequest.getStatus());
  }

  @Test
  void shouldUpdateItemInfoInMediatedRequestUponPrimaryRequestUpdate() {
    MediatedRequest mediatedRequest = buildMediatedRequest(
      MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
      .confirmedRequestId(CONFIRMED_REQUEST_ID.toString())
      .itemId(null)
      .holdingsRecordId(null)
      .item(null)
      .searchIndex(null);

    MediatedRequestEntity initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));

    assertNull(initialMediatedRequest.getItemId());
    assertNull(initialMediatedRequest.getHoldingsRecordId());
    assertNull(initialMediatedRequest.getItemBarcode());
    assertNull(initialMediatedRequest.getCallNumber());
    assertNull(initialMediatedRequest.getCallNumberPrefix());
    assertNull(initialMediatedRequest.getCallNumberSuffix());
    assertNull(initialMediatedRequest.getShelvingOrder());

    mockHelper.mockItemSearch(TENANT_ID_CENTRAL, ITEM_ID, new ConsortiumItem()
      .id(ITEM_ID)
      .tenantId(TENANT_ID_COLLEGE));

    Request primaryRequest = buildRequest(OPEN_NOT_YET_FILLED, CONFIRMED_REQUEST_ID)
      .holdingsRecordId(HOLDINGS_RECORD_ID)
      .itemId(ITEM_ID)
      .item(new RequestItem().barcode(ITEM_BARCODE));

    publishEventAndWait(TENANT_ID_SECURE, REQUEST_KAFKA_TOPIC_NAME,
      buildUpdateEvent(TENANT_ID_SECURE, primaryRequest, primaryRequest));

    MediatedRequestEntity updatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    // actual values come from mocked item-storage (resources/mappings/items.json)
    assertEquals(ITEM_ID, updatedRequest.getItemId().toString());
    assertEquals(HOLDINGS_RECORD_ID, updatedRequest.getHoldingsRecordId().toString());
    assertEquals(ITEM_BARCODE, updatedRequest.getItemBarcode());
    assertEquals("CN", updatedRequest.getCallNumber());
    assertEquals("PFX", updatedRequest.getCallNumberPrefix());
    assertEquals("SFX", updatedRequest.getCallNumberSuffix());
    assertEquals("CN vol.1 v.70:no.7-12 1984:July-Dec. cp.1 SFX", updatedRequest.getShelvingOrder());
  }

  @Test
  void shouldUpdateItemInfoInMediatedRequestUponPrimaryRequestUpdateWhenItemIsNotFoundUsingSearch() {
    MediatedRequest mediatedRequest = buildMediatedRequest(
      MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
      .confirmedRequestId(CONFIRMED_REQUEST_ID.toString())
      .itemId(null)
      .holdingsRecordId(null)
      .item(null)
      .searchIndex(null);

    MediatedRequestEntity initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));

    assertNull(initialMediatedRequest.getItemId());
    assertNull(initialMediatedRequest.getHoldingsRecordId());
    assertNull(initialMediatedRequest.getItemBarcode());
    assertNull(initialMediatedRequest.getCallNumber());
    assertNull(initialMediatedRequest.getCallNumberPrefix());
    assertNull(initialMediatedRequest.getCallNumberSuffix());
    assertNull(initialMediatedRequest.getShelvingOrder());

    mockHelper.mockItemSearch(TENANT_ID_CENTRAL, ITEM_ID, new ConsortiumItem()); // empty response

    Request primaryRequest = buildRequest(OPEN_NOT_YET_FILLED, CONFIRMED_REQUEST_ID)
      .holdingsRecordId(HOLDINGS_RECORD_ID)
      .itemId(ITEM_ID)
      .item(new RequestItem().barcode(ITEM_BARCODE));

    publishEventAndWait(TENANT_ID_SECURE, REQUEST_KAFKA_TOPIC_NAME,
      buildUpdateEvent(TENANT_ID_SECURE, primaryRequest, primaryRequest));

    MediatedRequestEntity updatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(ITEM_ID, updatedRequest.getItemId().toString());
    assertEquals(HOLDINGS_RECORD_ID, updatedRequest.getHoldingsRecordId().toString());
    assertEquals(ITEM_BARCODE, updatedRequest.getItemBarcode());
    assertNull(initialMediatedRequest.getCallNumber());
    assertNull(initialMediatedRequest.getCallNumberPrefix());
    assertNull(initialMediatedRequest.getCallNumberSuffix());
    assertNull(initialMediatedRequest.getShelvingOrder());
  }

  @Test
  void shouldUpdateItemInfoInMediatedRequestUponPrimaryRequestUpdateWhenItemIsNotFoundInStorage() {
    MediatedRequest mediatedRequest = buildMediatedRequest(
      MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
      .confirmedRequestId(CONFIRMED_REQUEST_ID.toString())
      .itemId(null)
      .holdingsRecordId(null)
      .item(null)
      .searchIndex(null);

    MediatedRequestEntity initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));

    assertNull(initialMediatedRequest.getItemId());
    assertNull(initialMediatedRequest.getHoldingsRecordId());
    assertNull(initialMediatedRequest.getItemBarcode());
    assertNull(initialMediatedRequest.getCallNumber());
    assertNull(initialMediatedRequest.getCallNumberPrefix());
    assertNull(initialMediatedRequest.getCallNumberSuffix());
    assertNull(initialMediatedRequest.getShelvingOrder());

    String itemId = randomId();

    mockHelper.mockItemSearch(TENANT_ID_CENTRAL, itemId, new ConsortiumItem()
      .id(itemId)
      .tenantId(TENANT_ID_COLLEGE));

    wireMockServer.stubFor(get(urlPathEqualTo(ITEM_STORAGE_URL + "/" + itemId))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(notFound()));

    Request primaryRequest = buildRequest(OPEN_NOT_YET_FILLED, CONFIRMED_REQUEST_ID)
      .holdingsRecordId(HOLDINGS_RECORD_ID)
      .itemId(itemId)
      .item(new RequestItem().barcode(ITEM_BARCODE));

    publishEventAndWait(TENANT_ID_SECURE, REQUEST_KAFKA_TOPIC_NAME,
      buildUpdateEvent(TENANT_ID_SECURE, primaryRequest, primaryRequest));

    MediatedRequestEntity updatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(itemId, updatedRequest.getItemId().toString());
    assertEquals(HOLDINGS_RECORD_ID, updatedRequest.getHoldingsRecordId().toString());
    assertEquals(ITEM_BARCODE, updatedRequest.getItemBarcode());
    assertNull(initialMediatedRequest.getCallNumber());
    assertNull(initialMediatedRequest.getCallNumberPrefix());
    assertNull(initialMediatedRequest.getCallNumberSuffix());
    assertNull(initialMediatedRequest.getShelvingOrder());
  }

  @Test
  void itemUpdateEventShouldBeIgnoredIfNewOrOldFieldISMissing() {
    var itemId = randomId();

    var mediatedRequestItemBarcode1 = "old_barcode_1";
    var mediatedRequest1 = createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(
      buildMediatedRequest(
        MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
        .itemId(itemId)
        .item(new MediatedRequestItem().barcode(mediatedRequestItemBarcode1))));

    var mediatedRequestItemBarcode2 = "old_barcode_2";
    var mediatedRequest2 = createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(
      buildMediatedRequest(
        MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
        .itemId(itemId)
        .item(new MediatedRequestItem().barcode(mediatedRequestItemBarcode2))));

    var oldNonEmptyBarcode = "old_barcode";
    var newBarcode = "new_barcode";
    publishEventAndWait(TENANT_ID_SECURE, ITEM_KAFKA_TOPIC_NAME,
      buildInventoryUpdateEvent(TENANT_ID_SECURE, null, buildItem(itemId, newBarcode)));
    publishEventAndWait(TENANT_ID_SECURE, ITEM_KAFKA_TOPIC_NAME,
      buildInventoryUpdateEvent(TENANT_ID_SECURE, buildItem(itemId, oldNonEmptyBarcode), null));

    MediatedRequestEntity updatedRequest1 = getMediatedRequest(mediatedRequest1.getId());
    assertEquals(mediatedRequestItemBarcode1, updatedRequest1.getItemBarcode());

    MediatedRequestEntity updatedRequest2 = getMediatedRequest(mediatedRequest2.getId());
    assertEquals(mediatedRequestItemBarcode2, updatedRequest2.getItemBarcode());
  }

  @Test
  void itemUpdateEventShouldBeIgnoredIfBarcodeWasAlreadyNotEmpty() {
    var itemId = randomId();

    var mediatedRequestItemBarcode1 = "old_barcode_1";
    var mediatedRequest1 = createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(
      buildMediatedRequest(
        MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
        .itemId(itemId)
        .item(new MediatedRequestItem().barcode(mediatedRequestItemBarcode1))));

    var mediatedRequestItemBarcode2 = "old_barcode_2";
    var mediatedRequest2 = createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(
      buildMediatedRequest(
        MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
        .itemId(itemId)
        .item(new MediatedRequestItem().barcode(mediatedRequestItemBarcode2))));

    var oldNonEmptyBarcode = "old_barcode";
    var newBarcode = "new_barcode";
    publishEventAndWait(TENANT_ID_SECURE, ITEM_KAFKA_TOPIC_NAME,
      buildInventoryUpdateEvent(TENANT_ID_SECURE, buildItem(itemId, oldNonEmptyBarcode),
        buildItem(itemId, newBarcode)));

    MediatedRequestEntity updatedRequest1 = getMediatedRequest(mediatedRequest1.getId());
    assertEquals(mediatedRequestItemBarcode1, updatedRequest1.getItemBarcode());

    MediatedRequestEntity updatedRequest2 = getMediatedRequest(mediatedRequest2.getId());
    assertEquals(mediatedRequestItemBarcode2, updatedRequest2.getItemBarcode());
  }

  @Test
  void mediatedRequestItemBarcodeShouldBeUpdatedWhenItemUpdated() {
    var itemId = randomId();

    var mediatedRequest1 = createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(
      buildMediatedRequest(
        MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
        .itemId(itemId)
        .item(new MediatedRequestItem().barcode("old_barcode_1"))));

    var mediatedRequest2 = createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(
      buildMediatedRequest(
        MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
        .itemId(itemId)
        .item(new MediatedRequestItem().barcode("old_barcode_2"))));

    var newBarcode = "new_barcode";
    publishEventAndWait(TENANT_ID_SECURE, ITEM_KAFKA_TOPIC_NAME,
      buildInventoryUpdateEvent(TENANT_ID_SECURE, buildItem(itemId, null),
        buildItem(itemId, newBarcode)));

    MediatedRequestEntity updatedRequest1 = getMediatedRequest(mediatedRequest1.getId());
    assertEquals(newBarcode, updatedRequest1.getItemBarcode());

    MediatedRequestEntity updatedRequest2 = getMediatedRequest(mediatedRequest2.getId());
    assertEquals(newBarcode, updatedRequest2.getItemBarcode());
  }

  @Test
  void shouldNotFailIfUserIdInHeaderIsNull() {
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_NOT_YET_FILLED, OPEN_IN_TRANSIT);

    assertDoesNotThrow(() -> publishEventAndWaitWithHeaders(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event,
        Map.of(XOkapiHeaders.USER_ID, "test-user-id".getBytes(StandardCharsets.UTF_8))));

    assertDoesNotThrow(() -> publishEventAndWaitWithHeaders(TENANT_ID_COLLEGE, ITEM_KAFKA_TOPIC_NAME, event, Map.of()));
  }

  private void publishEventAndWaitWithHeaders(String tenant, String topic, KafkaEvent<?> event,
      Map<String, byte[]> additionalHeaders) {

    final int initialOffset = getOffset(topic, CONSUMER_GROUP_ID);
    publishEventWithHeaders(tenant, topic, asJsonString(event), additionalHeaders);
    waitForOffset(topic, CONSUMER_GROUP_ID, initialOffset + 1);
  }

  @SneakyThrows
  private void publishEventWithHeaders(String tenant, String topic, String payload,
      Map<String, byte[]> additionalHeaders) {

    Collection<Header> headers = buildHeadersForKafkaProducer(tenant);
    additionalHeaders.forEach((key, value) -> headers.add(new RecordHeader(key, value)));
    kafkaTemplate.send(new ProducerRecord<>(topic, 0, randomId(), payload, headers))
      .get(10, SECONDS);
  }

  private static KafkaEvent<Request> buildRequestUpdateEvent(Request.StatusEnum oldStatus,
    Request.StatusEnum newStatus) {
    return buildUpdateEvent(TENANT_ID_CONSORTIUM,
      buildRequest(oldStatus, CONFIRMED_REQUEST_ID),
      buildRequest(newStatus, CONFIRMED_REQUEST_ID));
  }

  private static KafkaEvent<Request> buildLocalRequestUpdateEvent(Request.StatusEnum oldStatus,
    Request.StatusEnum newStatus) {
    return buildUpdateEvent(TENANT_ID_CONSORTIUM,
      buildRequest(oldStatus, CONFIRMED_REQUEST_ID, null),
      buildRequest(newStatus, CONFIRMED_REQUEST_ID, null));
  }

  private static <T> KafkaEvent<T> buildUpdateEvent(String tenant, T oldVersion, T newVersion) {
    return buildEvent(tenant, DefaultKafkaEvent.DefaultKafkaEventType.UPDATED,
      oldVersion, newVersion);
  }

  private static <T> KafkaEvent<T> buildInventoryUpdateEvent(String tenant, T oldVersion,
    T newVersion) {

    return buildInventoryEvent(tenant, InventoryKafkaEvent.InventoryKafkaEventType.UPDATE,
      oldVersion, newVersion);
  }

  private <T> void publishEventAndWait(String tenant, String topic, KafkaEvent<T> event) {
    publishEventAndWait(tenant, topic, asJsonString(event));
  }

  private void publishEventAndWait(String tenant, String topic, String payload) {
    final int initialOffset = getOffset(topic, CONSUMER_GROUP_ID);
    publishEvent(tenant, topic, payload);
    waitForOffset(topic, CONSUMER_GROUP_ID, initialOffset + 1);
  }

  @SneakyThrows
  private void publishEvent(String tenant, String topic, String payload) {
    Collection<Header> headers = buildHeadersForKafkaProducer(tenant);
    kafkaTemplate.send(new ProducerRecord<>(topic, 0, randomId(), payload, headers))
      .get(10, SECONDS);
  }

  private void waitForOffset(String topic, String consumerGroupId, int expectedOffset) {
    Awaitility.await()
      .atMost(60, TimeUnit.SECONDS)
      .until(() -> getOffset(topic, consumerGroupId), offset -> offset.equals(expectedOffset));
  }

  @SneakyThrows
  private static int getOffset(String topic, String consumerGroup) {
    return kafkaAdminClient.listConsumerGroupOffsets(consumerGroup)
      .partitionsToOffsetAndMetadata()
      .thenApply(partitions -> Optional.ofNullable(partitions.get(new TopicPartition(topic, 0)))
        .map(OffsetAndMetadata::offset)
        .map(Long::intValue)
        .orElse(0))
      .get(10, TimeUnit.SECONDS);
  }

  private static Request buildRequest(Request.StatusEnum status, UUID requestId) {
    return buildRequest(status, requestId, Request.EcsRequestPhaseEnum.PRIMARY);
  }

  private static Request buildRequest(Request.StatusEnum status,
    UUID requestId, Request.EcsRequestPhaseEnum ecsPhase) {
    return new Request()
      .id(requestId.toString())
      .ecsRequestPhase(ecsPhase)
      .requestLevel(Request.RequestLevelEnum.TITLE)
      .requestType(Request.RequestTypeEnum.HOLD)
      .requestDate(new Date())
      .status(status)
      .position(1)
      .itemId(ITEM_ID)
      .instance(new RequestInstance().title("Test title"))
      .item(new RequestItem().barcode("test"))
      .cancellationReasonId(UUID.randomUUID().toString())
      .cancelledDate(new Date())
      .cancelledByUserId(UUID.randomUUID().toString())
      .requester(new RequestRequester()
        .firstName("First")
        .lastName("Last")
        .barcode("test"))
      .fulfillmentPreference(Request.FulfillmentPreferenceEnum.HOLD_SHELF);
  }

  private static Item buildItem(String id, String barcode) {
    return new Item()
      .id(id)
      .barcode(barcode);
  }

  private MediatedRequestEntity createMediatedRequest(MediatedRequestEntity ecsTlr) {
    ecsTlr.setId(null);
    return executionService.executeSystemUserScoped(TENANT_ID_CONSORTIUM,
      () -> mediatedRequestsRepository.save(ecsTlr));
  }

  private MediatedRequestEntity getMediatedRequest(UUID id) {
    return executionService.executeSystemUserScoped(TENANT_ID_CONSORTIUM,
      () -> mediatedRequestsRepository.findById(id)).orElseThrow();
  }
}
