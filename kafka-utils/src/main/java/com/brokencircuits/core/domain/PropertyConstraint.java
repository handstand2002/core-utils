package com.brokencircuits.core.domain;

import java.util.Collection;
import lombok.Builder;

@Builder
public class PropertyConstraint {

  private Class<?> type;
  private Collection<Object> possibleSelection;
}
