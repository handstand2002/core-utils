package com.brokencircuits.core.utils.kafka;

import com.brokencircuits.core.Service;
import com.brokencircuits.core.domain.kafka.Topic;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class KafkaBgConsumer<K, V> implements Service {

  public final static String CONFIG_CONNECT_TIMEOUT = "connect.timeout.ms";

  private final Properties kafkaConsumerProperties;
  private final Properties bgConsumerProperties;
  private final Topic<K, V> topic;
  private final java.util.function.Consumer<ConsumerRecord<K, V>> onRecord;
  private Consumer<K, V> consumer;
  private Thread receiveThread;

  public KafkaBgConsumer(Properties kafkaConsumerProperties,
      Properties bgConsumerProperties, Topic<K, V> topic,
      java.util.function.Consumer<ConsumerRecord<K, V>> onRecord) {
    this.kafkaConsumerProperties = kafkaConsumerProperties;
    this.bgConsumerProperties = bgConsumerProperties;
    this.topic = topic;
    this.onRecord = onRecord;
    if (kafkaConsumerProperties.containsKey(ConsumerConfig.GROUP_ID_CONFIG)) {
      log.warn("Removing config {} due to incompatibility with {}", ConsumerConfig.GROUP_ID_CONFIG,
          getClass().getSimpleName());
      kafkaConsumerProperties.remove(ConsumerConfig.GROUP_ID_CONFIG);
    }

    if (!bgConsumerProperties.containsKey(CONFIG_CONNECT_TIMEOUT)) {
      bgConsumerProperties.put(CONFIG_CONNECT_TIMEOUT, 30000);
    }

    validateProperties();
  }

  private void validateProperties() {
    Properties propClone = new Properties(bgConsumerProperties);
    propClone.remove(CONFIG_CONNECT_TIMEOUT);
    propClone.forEach((k, v) -> log.warn("config supplied but not recognized: {}: {}", k, v));
  }

  public List<TopicPartition> getPartitions()
      throws ExecutionException, InterruptedException, TimeoutException {
    AdminClient admin = KafkaAdminClient.create(kafkaConsumerProperties);
    DescribeTopicsResult topicConfig = admin
        .describeTopics(Collections.singleton(topic.getName()));

    long connectTimeoutMs = Long
        .parseLong(bgConsumerProperties.getProperty(CONFIG_CONNECT_TIMEOUT));

    return topicConfig.all().get(connectTimeoutMs, TimeUnit.MILLISECONDS).get(topic.getName())
        .partitions().parallelStream()
        .map(info -> new TopicPartition(topic.getName(), info.partition()))
        .collect(Collectors.toList());
  }

  public Consumer<K, V> createConsumer()
      throws InterruptedException, ExecutionException, TimeoutException {

    Consumer<K, V> consumer = new KafkaConsumer<>(kafkaConsumerProperties);
    List<TopicPartition> partitions = getPartitions();
    consumer.assign(partitions);

    return consumer;
  }

  @Override
  public void start() throws InterruptedException, ExecutionException, TimeoutException {
    consumer = createConsumer();

    receiveThread = new Thread(() -> {
      while (true) {
        ConsumerRecords<K, V> records = consumer.poll(Duration.ofSeconds(30));
        records.forEach(onRecord);
      }
    });

    receiveThread.start();
  }

  @Override
  public void stop() {
    receiveThread.interrupt();
  }
}
