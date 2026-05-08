package com.github.jelatinone.api.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.jelatinone.api.Index;
import com.github.jelatinone.api.Property;

public record EdgeCritiera<Identifier>(
    Optional<Identifier> id,
    Optional<Identifier> from,
    Optional<Identifier> to,

    GraphDirection direction,

    Map<Index<?>, Property> queryProperties) {

  public static <Key> EdgeCritiera<Key> id(Key id) {
    return new EdgeCritiera<>(
        Optional.of(id),
        Optional.empty(),
        Optional.empty(),
        GraphDirection.ANY,
        Map.of());
  }

  public static <Key> EdgeCritiera<Key> from(Key from) {
    return new EdgeCritiera<>(
        Optional.empty(),
        Optional.of(from),
        Optional.empty(),
        GraphDirection.OUT,
        Map.of());
  }

  public static <Key> EdgeCritiera<Key> to(Key to) {
    return new EdgeCritiera<>(
        Optional.empty(),
        Optional.empty(),
        Optional.of(to),
        GraphDirection.IN,
        Map.of());
  }

  public static <Key> EdgeCritiera<Key> between(Key from, Key to) {
    return new EdgeCritiera<>(
        Optional.empty(),
        Optional.of(from),
        Optional.of(to),
        GraphDirection.OUT,
        Map.of());
  }

  public EdgeCritiera<Identifier> withProperty(Index<?> key, Property value) {
    Map<Index<?>, Property> next = new HashMap<>(queryProperties) {
      {
        put(key, value);
      }
    };

    return new EdgeCritiera<>(
        id,
        from,
        to,
        direction,
        Map.copyOf(next));
  }
}