package com.brokencircuits.core.domain.kafka;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.kafka.common.serialization.Serde;

@RequiredArgsConstructor
@Value
public class Topic<K,V> {
  private final String name;
  private final Serde<K> keySerde;
  private final Serde<V> valueSerde;
}
