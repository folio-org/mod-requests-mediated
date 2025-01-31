package org.folio.mr.controller;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.folio.mr.domain.dto.Request.StatusEnum.CLOSED_CANCELLED;
import static org.folio.mr.domain.dto.Request.StatusEnum.CLOSED_FILLED;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_AWAITING_PICKUP;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_IN_TRANSIT;
import static org.folio.mr.domain.dto.Request.StatusEnum.OPEN_NOT_YET_FILLED;
import static org.folio.mr.support.KafkaEvent.EventType.UPDATED;
import static org.folio.mr.util.TestEntityBuilder.buildMediatedRequest;
import static org.folio.mr.util.TestUtils.buildEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.awaitility.Awaitility;
import org.folio.mr.api.BaseIT;
import org.folio.mr.config.TenantConfig;
import org.folio.mr.domain.dto.MediatedRequest;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.domain.dto.RequestInstance;
import org.folio.mr.domain.dto.RequestItem;
import org.folio.mr.domain.dto.RequestRequester;
import org.folio.mr.domain.entity.MediatedRequestEntity;
import org.folio.mr.domain.mapper.MediatedRequestMapperImpl;
import org.folio.mr.repository.MediatedRequestsRepository;
import org.folio.mr.support.KafkaEvent;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.SneakyThrows;
import org.springframework.boot.test.mock.mockito.MockBean;

class KafkaEventListenerTest extends BaseIT {
  private static final String CONSUMER_GROUP_ID = "folio-mod-requests-mediated-group";
  private static final UUID CONFIRMED_REQUEST_ID = randomUUID();
  private final MediatedRequestMapperImpl mediatedRequestMapper = new MediatedRequestMapperImpl();
  @Autowired
  private MediatedRequestsRepository mediatedRequestsRepository;
  @Autowired
  private SystemUserScopedExecutionService executionService;
  @MockBean
  private TenantConfig tenantConfig;

  @BeforeEach
  void beforeEach() {
    mediatedRequestsRepository.deleteAll();
  }

  @Test
  void shouldCancelMediatedRequestUponConfirmedRequestCancel() {
    when(tenantConfig.getSecureTenantId()).thenReturn(TENANT_ID_CONSORTIUM);
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_NOT_YET_FILLED, CLOSED_CANCELLED);
    MediatedRequest mediatedRequest =
      buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
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
    when(tenantConfig.getSecureTenantId()).thenReturn(TENANT_ID_CONSORTIUM);
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_IN_TRANSIT, OPEN_AWAITING_PICKUP);
    MediatedRequest mediatedRequest =
      buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_IN_TRANSIT_TO_BE_CHECKED_OUT);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.OPEN_AWAITING_PICKUP.getValue(), updatedMediatedRequest.getStatus());
  }

  @Test
  void shouldCancelMediatedRequestUponConfirmedRequestFill() {
    when(tenantConfig.getSecureTenantId()).thenReturn(TENANT_ID_CONSORTIUM);
    KafkaEvent<Request> event = buildRequestUpdateEvent(OPEN_NOT_YET_FILLED, CLOSED_FILLED);
    MediatedRequest mediatedRequest =
      buildMediatedRequest(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED);
    mediatedRequest.setConfirmedRequestId(CONFIRMED_REQUEST_ID.toString());
    var initialMediatedRequest =
      createMediatedRequest(mediatedRequestMapper.mapDtoToEntity(mediatedRequest));
    assertNotNull(initialMediatedRequest.getId());

    publishEventAndWait(TENANT_ID_CONSORTIUM, REQUEST_KAFKA_TOPIC_NAME, event);

    MediatedRequestEntity updatedMediatedRequest = getMediatedRequest(initialMediatedRequest.getId());
    assertEquals(MediatedRequest.StatusEnum.CLOSED_FILLED.getValue(), updatedMediatedRequest.getStatus());
  }

  private static KafkaEvent<Request> buildRequestUpdateEvent(Request.StatusEnum oldStatus,
    Request.StatusEnum newStatus) {
    return buildUpdateEvent(TENANT_ID_CONSORTIUM,
      buildRequest(oldStatus, CONFIRMED_REQUEST_ID),
      buildRequest(newStatus, CONFIRMED_REQUEST_ID));
  }

  private static <T> KafkaEvent<T> buildUpdateEvent(String tenant, T oldVersion, T newVersion) {
    return buildEvent(tenant, UPDATED, oldVersion, newVersion);
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
    kafkaTemplate.send(new ProducerRecord<>(topic, 0, randomId(), payload,
        List.of(
          new RecordHeader(XOkapiHeaders.TENANT, tenant.getBytes()),
          new RecordHeader("folio.tenantId", randomId().getBytes())
        )))
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
    return new Request()
      .id(requestId.toString())
      .requestLevel(Request.RequestLevelEnum.TITLE)
      .requestType(Request.RequestTypeEnum.HOLD)
      .requestDate(new Date())
      .status(status)
      .position(1)
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

  private MediatedRequestEntity createMediatedRequest(MediatedRequestEntity ecsTlr) {
    return executionService.executeSystemUserScoped(TENANT_ID_CONSORTIUM,
      () -> mediatedRequestsRepository.save(ecsTlr));
  }

  private MediatedRequestEntity getMediatedRequest(UUID id) {
    return executionService.executeSystemUserScoped(TENANT_ID_CONSORTIUM,
      () -> mediatedRequestsRepository.findById(id)).orElseThrow();
  }
}
