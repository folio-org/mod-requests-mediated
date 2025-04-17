package org.folio.mr.service;


import org.folio.mr.support.kafka.KafkaEvent;

public interface KafkaEventHandler<T> {
  void handle(KafkaEvent<T> event);
}
