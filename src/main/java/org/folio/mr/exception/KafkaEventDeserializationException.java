package org.folio.mr.exception;

public class KafkaEventDeserializationException extends RuntimeException {
  public KafkaEventDeserializationException(Throwable cause) {
    super(cause);
  }

  public KafkaEventDeserializationException(String message) {
    super(message);
  }
}
