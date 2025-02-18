package org.folio.mr.config;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class KafkaConsumerConfig {

  @Bean
  public ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties) {
    log.debug("consumerFactory:: {}", kafkaProperties);

    Map<String, Object> consumerConfig = Map.of(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(),
      ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId(),
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getConsumer().getAutoOffsetReset(),
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,

      ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50,

      ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 1048576,
      ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 524288
    );

    return new DefaultKafkaConsumerFactory<>(consumerConfig);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
    KafkaProperties kafkaProperties) {

    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory(kafkaProperties));
    factory.setConcurrency(3);

    return factory;
  }

}
