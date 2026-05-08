package com.github.jelatinone.api.graph;

import java.util.HashMap;
import java.util.Map;

import com.github.jelatinone.api.Index;
import com.github.jelatinone.api.Property;

public record GraphCriteria<Identifier>(
    Identifier origin,

    GraphDirection queryDirection,

    int originRadius,
    boolean originIncluded,

    Map<Index<?>, Property> queryProperties) {

  public GraphCriteria {
    if (originRadius < 1) {
      throw new IllegalArgumentException("originRadius must be positive");
    }
  }

  public static <Identifier> GraphCriteria<Identifier> outgoing(Identifier origin) {
    return new GraphCriteria<Identifier>(
        origin,
        GraphDirection.OUT,
        1,
        false,
        Map.of());
  }

  public static <Identifier> GraphCriteria<Identifier> incoming(Identifier origin) {
    return new GraphCriteria<Identifier>(
        origin,
        GraphDirection.IN,
        1,
        false,
        Map.of());
  }

  public static <Identifier> GraphCriteria<Identifier> any(Identifier origin) {
    return new GraphCriteria<Identifier>(
        origin,
        GraphDirection.ANY,
        1,
        false,
        Map.of());
  }

  public GraphCriteria<Identifier> originRadius(int originRadius) {
    return new GraphCriteria<Identifier>(
        origin,
        queryDirection,
        originRadius,
        originIncluded,
        queryProperties);
  }

  public GraphCriteria<Identifier> originIncluded(boolean include) {
    return new GraphCriteria<Identifier>(
        origin,
        queryDirection,
        originRadius,
        include,
        queryProperties);
  }

  public GraphCriteria<Identifier> withProperty(Index<?> key, Property value) {
    Map<Index<?>, Property> next = new HashMap<>(queryProperties) {
      {
        put(key, value);
      }
    };

    return new GraphCriteria<Identifier>(
        origin,
        queryDirection,
        originRadius,
        originIncluded,
        Map.copyOf(next));
  }
}