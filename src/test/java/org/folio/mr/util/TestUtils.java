package org.folio.mr.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.folio.mr.support.KafkaEvent;
import org.json.JSONObject;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {

  @SneakyThrows
  public static String buildToken(String tenantId) {
    JSONObject header = new JSONObject()
      .put("alg", "HS256");

    JSONObject payload = new JSONObject()
      .put("sub", tenantId + "_admin")
      .put("user_id", "bb6a6f19-9275-4261-ad9d-6c178c24c4fb")
      .put("type", "access")
      .put("exp", 1708342543)
      .put("iat", 1708341943)
      .put("tenant", tenantId);

    String signature = "De_0um7P_Rv-diqjHKLcSHZdjzjjshvlBbi6QPrz0Tw";

    return String.format("%s.%s.%s",
      Base64.getEncoder().encodeToString(header.toString().getBytes()),
      Base64.getEncoder().encodeToString(payload.toString().getBytes()),
      signature);
  }

  public static <T> KafkaEvent<T> buildEvent(String tenant, KafkaEvent.EventType type,
                                             T oldVersion, T newVersion) {

    KafkaEvent.EventData<T> data = KafkaEvent.EventData.<T>builder()
      .oldVersion(oldVersion)
      .newVersion(newVersion)
      .build();

    return buildEvent(tenant, type, data);
  }

  private static <T> KafkaEvent<T> buildEvent(String tenant, KafkaEvent.EventType type,
                                              KafkaEvent.EventData<T> data) {

    return KafkaEvent.<T>builder()
      .id(UUID.randomUUID().toString())
      .type(type)
      .timestamp(new Date().getTime())
      .tenant(tenant)
      .data(data)
      .build();
  }

  public static String dateToString(Date date) {
    return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"))
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx"));
  }
}
