package com.github.jelatinone.api.graph;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Index;
import com.github.jelatinone.api.Property;

public interface EdgeCritiera<Identifier> extends Criteria<Identifier> {

  Optional<Identifier> from();

  Optional<Identifier> to();

  GraphDirection direction();

  public static <Key> EdgeCritiera<Key> identifier(Key id) {
    return new DefaultEdgeCriteria<>(
        Optional.of(id),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        GraphDirection.ANY,
        Map.of());
  }

  public static <Key> EdgeCritiera<Key> from(Key from) {
    return new DefaultEdgeCriteria<>(
        Optional.empty(),
        Optional.empty(),
        Optional.of(from),
        Optional.empty(),
        GraphDirection.OUT,
        Map.of());
  }

  public static <Key> EdgeCritiera<Key> to(Key to) {
    return new DefaultEdgeCriteria<>(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(to),
        GraphDirection.IN,
        Map.of());
  }

  public static <Key> EdgeCritiera<Key> between(Key from, Key to) {
    return new DefaultEdgeCriteria<>(
        Optional.empty(),
        Optional.empty(),
        Optional.of(from),
        Optional.of(to),
        GraphDirection.OUT,
        Map.of());
  }
}

record DefaultEdgeCriteria<Identifier>(
    Optional<Identifier> identifier,
    Optional<Duration> duration,
    Optional<Identifier> from,
    Optional<Identifier> to,
    GraphDirection direction,
    Map<Index<?>, Property> queryProperties) implements EdgeCritiera<Identifier> {
}
