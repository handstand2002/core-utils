package com.brokencircuits.core.utils.kafka;

import com.brokencircuits.core.domain.PropertyConstraint;
import com.brokencircuits.core.domain.kafka.Topic;
import com.brokencircuits.core.utils.FluentHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartitionInfo;

@Slf4j
public class KafkaBgConsumer<K, V> {

  public final static String CONFIG_CONNECT_TIMEOUT = "connect.timeout.ms";
  private final static FluentHashMap<String, PropertyConstraint> PROPERTY_CONSTRAINTS = new FluentHashMap<String, PropertyConstraint>()
      .add(CONFIG_CONNECT_TIMEOUT, PropertyConstraint.builder().type(Long.class).build());

  private final Properties kafkaConsumerProperties;
  private final Properties bgConsumerProperties;
  private final Topic<K, V> topic;
  private final boolean doCommit;

  public KafkaBgConsumer(Properties kafkaConsumerProperties,
      Properties bgConsumerProperties, Topic<K, V> topic) {
    this.kafkaConsumerProperties = kafkaConsumerProperties;
    this.bgConsumerProperties = bgConsumerProperties;
    this.topic = topic;
    doCommit = kafkaConsumerProperties.containsKey(ConsumerConfig.GROUP_ID_CONFIG);

    updateProperties();
    validateProperties();
  }

  private void updateProperties() {
    if ()
  }

  private void validateProperties() {
    Properties propClone = new Properties(bgConsumerProperties);
    propClone.remove(CONFIG_CONNECT_TIMEOUT);
    propClone.forEach((k, v) -> log.warn("config supplied but not recognized: {}: {}", k, v));
  }

  public List<TopicPartitionInfo> getPartitions()
      throws ExecutionException, InterruptedException, TimeoutException {
    AdminClient admin = KafkaAdminClient.create(kafkaConsumerProperties);
    DescribeTopicsResult topicConfig = admin
        .describeTopics(Collections.singleton(topic.getName()));

    long connectTimeoutMs = Long
        .parseLong(bgConsumerProperties.getProperty(CONFIG_CONNECT_TIMEOUT));

    return topicConfig.all().get(connectTimeoutMs, TimeUnit.MILLISECONDS).get(topic.getName())
        .partitions();
  }

  public Consumer<Long, String> createConsumer() {

    Consumer<Long, String> consumer = new KafkaConsumer<>(kafkaConsumerProperties);
    consumer.subscribe(Collections.singletonList(topic.getName()));
    return consumer;
  }
}
