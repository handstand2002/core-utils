package com.brokencircuits.core.utils;

import java.util.HashMap;

public class FluentHashMap<K,V> extends HashMap<K,V> {

  public FluentHashMap<K,V> add(K key, V value) {
    put(key, value);
    return this;
  }
}
