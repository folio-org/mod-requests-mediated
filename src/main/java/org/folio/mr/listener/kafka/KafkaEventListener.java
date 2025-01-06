package org.folio.mr.listener.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.folio.mr.config.TenantConfig;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.exception.KafkaEventDeserializationException;
import org.folio.mr.service.KafkaEventHandler;
import org.folio.mr.service.impl.RequestEventHandler;
import org.folio.mr.support.KafkaEvent;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class KafkaEventListener {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final RequestEventHandler requestEventHandler;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final TenantConfig secureTenantConfig;

  public KafkaEventListener(@Autowired RequestEventHandler requestEventHandler,
    @Autowired SystemUserScopedExecutionService systemUserScopedExecutionService, TenantConfig secureTenantConfig) {

    this.requestEventHandler = requestEventHandler;
    this.systemUserScopedExecutionService = systemUserScopedExecutionService;
    this.secureTenantConfig = secureTenantConfig;
  }

  @KafkaListener(
    topicPattern = "${folio.environment}\\.\\w+\\.circulation\\.request",
    groupId = "${spring.kafka.consumer.group-id}"
  )
  public void handleRequestEvent(String eventString, MessageHeaders messageHeaders) {
    handleEvent(eventString, requestEventHandler, messageHeaders, Request.class);
  }

  private <T> void handleEvent(String eventString, KafkaEventHandler<T> handler,
    Map<String, Object> messageHeaders, Class<T> payloadType) {

    log.debug("handleEvent:: event: {}", () -> eventString);
    KafkaEvent<T> event = deserialize(eventString, messageHeaders, payloadType);
    log.info("handleEvent:: event received: {}", event.getId());

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(secureTenantConfig.getSecureTenantId(),
      () -> handler.handle(event));
    log.info("handleEvent:: event consumed: {}", event.getId());
  }

  private static <T> KafkaEvent<T> deserialize(String eventString, Map<String, Object> messageHeaders,
    Class<T> dataType) {

    try {
      JavaType eventType = objectMapper.getTypeFactory()
        .constructParametricType(KafkaEvent.class, dataType);
      var kafkaEvent = objectMapper.<KafkaEvent<T>>readValue(eventString, eventType);
      return Optional.ofNullable(getHeaderValue(messageHeaders, XOkapiHeaders.TENANT))
        .map(kafkaEvent::withTenantIdHeaderValue)
        .orElseThrow(() -> new KafkaEventDeserializationException(
          "Failed to get tenant ID from message headers"));
    } catch (JsonProcessingException e) {
      log.error("deserialize:: failed to deserialize event", e);
      throw new KafkaEventDeserializationException(e);
    }
  }

  private static String getHeaderValue(Map<String, Object> headers, String headerName) {
    log.debug("getHeaderValue:: headers: {}, headerName: {}", () -> headers, () -> headerName);
    var headerValue = headers.get(headerName);
    var value = headerValue == null
      ? null
      : new String((byte[]) headerValue, StandardCharsets.UTF_8);
    log.info("getHeaderValue:: header {} value is {}", headerName, value);
    return value;
  }
}
