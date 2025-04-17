package org.folio.mr.listener.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.folio.mr.config.TenantConfig;
import org.folio.mr.domain.dto.Request;
import org.folio.mr.exception.KafkaEventDeserializationException;
import org.folio.mr.service.KafkaEventHandler;
import org.folio.mr.service.impl.RequestEventHandler;
import org.folio.mr.support.kafka.DefaultKafkaEvent;
import org.folio.mr.support.kafka.KafkaEvent;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class KafkaEventListener {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final RequestEventHandler requestEventHandler;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final TenantConfig secureTenantConfig;
  private final FolioModuleMetadata folioModuleMetadata;

  @KafkaListener(
    topicPattern = "${folio.environment}\\.\\w+\\.circulation\\.request",
    groupId = "${spring.kafka.consumer.group-id}"
  )
  public void handleRequestEvent(String eventString, MessageHeaders messageHeaders) {
    handleEvent(eventString, requestEventHandler, messageHeaders, DefaultKafkaEvent.class, Request.class);
  }

  private <E, T> void handleEvent(String eventString, KafkaEventHandler<T> handler,
    Map<String, Object> messageHeaders, Class<E> kafkaEventClass, Class<T> payloadType) {

    log.debug("handleEvent:: event: {}", () -> eventString);
    KafkaEvent<T> event = deserialize(eventString, messageHeaders, kafkaEventClass, payloadType);
    log.info("handleEvent:: event received: {}", event::getId);

    FolioExecutionContext context = DefaultFolioExecutionContext.fromMessageHeaders(
      folioModuleMetadata, messageHeaders);

    try (FolioExecutionContextSetter ignored = new FolioExecutionContextSetter(context)) {
      systemUserScopedExecutionService.executeAsyncSystemUserScoped(secureTenantConfig.getSecureTenantId(),
        () -> handler.handle(event));
    } catch (Exception e) {
      log.error("handleEvent:: failed to handle event {}", event.getId(), e);
    }
    log.info("handleEvent:: event consumed: {}", event::getId);
  }

  private static <E, T> KafkaEvent<T> deserialize(String eventString, Map<String, Object> messageHeaders,
    Class<E> kafkaEventClass, Class<T> dataType) {

    try {
      JavaType eventType = objectMapper
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .getTypeFactory()
        .constructParametricType(kafkaEventClass, dataType);
      return objectMapper.<KafkaEvent<T>>readValue(eventString, eventType)
        .withTenantIdHeaderValue(getHeaderValue(messageHeaders, XOkapiHeaders.TENANT))
        .withUserIdHeaderValue(getHeaderValue(messageHeaders, XOkapiHeaders.USER_ID));
    } catch (JsonProcessingException e) {
      log.error("deserialize:: failed to deserialize event", e);
      throw new KafkaEventDeserializationException(e);
    }
  }

  private static String getHeaderValue(Map<String, Object> headers, String headerName) {
    log.debug("getHeaderValue:: headers: {}, headerName: {}", () -> headers, () -> headerName);
    var headerValue = headers.get(headerName);
    if (headerValue == null) {
      throw new KafkaEventDeserializationException(
        String.format("Failed to get [%s] from message headers", headerName));
    }
    var value = new String((byte[]) headerValue, StandardCharsets.UTF_8);
    log.info("getHeaderValue:: header {} value is {}", headerName, value);
    return value;
  }
}
