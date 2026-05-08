package com.github.jelatinone.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Criteria<Identifier>(
    Optional<Identifier> id,

    Map<Index<?>, Property> queryProperties) {
  public static <Identifier> Criteria<Identifier> id(Identifier id) {
    return new Criteria<>(
        Optional.of(id),
        Map.of());
  }

  public static <Identifier> Criteria<Identifier> property(Index<?> key, Property property) {
    return new Criteria<>(
        Optional.empty(),
        Map.of(key, property));
  }

  public Criteria<Identifier> withProperty(Index<?> key, Property property) {
    Map<Index<?>, Property> next = new HashMap<>(queryProperties) {
      {
        put(key, property);
      }
    };

    return new Criteria<>(
        id,
        Map.copyOf(next));
  }
}
