package com.github.jelatinone.api.graph;

import java.time.Duration;
import java.util.Optional;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.model.struct.Identity;

public interface EdgeCriteria<Identifier extends Identity> extends Criteria<Identifier> {

  Optional<Identifier> from();

  Optional<Identifier> to();

  GraphDirection direction();

  public static <Key extends Identity> EdgeCriteria<Key> identifier(Key id) {
    return new DefaultEdgeCriteria<>(
        Optional.of(id),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        GraphDirection.ANY);
  }

  public static <Key extends Identity> EdgeCriteria<Key> from(Key from) {
    return new DefaultEdgeCriteria<>(
        Optional.empty(),
        Optional.empty(),
        Optional.of(from),
        Optional.empty(),
        GraphDirection.OUT);
  }

  public static <Key extends Identity> EdgeCriteria<Key> to(Key to) {
    return new DefaultEdgeCriteria<>(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(to),
        GraphDirection.IN);
  }

  public static <Key extends Identity> EdgeCriteria<Key> between(Key from, Key to) {
    return new DefaultEdgeCriteria<>(
        Optional.empty(),
        Optional.empty(),
        Optional.of(from),
        Optional.of(to),
        GraphDirection.OUT);
  }
}

record DefaultEdgeCriteria<Identifier extends Identity>(
    Optional<Identifier> identifier,
    Optional<Duration> duration,
    Optional<Identifier> from,
    Optional<Identifier> to,
    GraphDirection direction) implements EdgeCriteria<Identifier> {
}
