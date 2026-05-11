package com.github.jelatinone.api.graph;

import java.time.Duration;
import java.util.Optional;

import com.github.jelatinone.api.Criteria;

public interface GraphCriteria<Identifier> extends Criteria<Identifier> {

  int originRadius();

  boolean originIncluded();

  GraphDirection direction();

  public static <Identifier> GraphCriteria<Identifier> outgoing(Identifier identifier) {
    return new DefaultGraphCriteria<Identifier>(
        Optional.of(identifier),
        Optional.empty(),
        1,
        false,
        GraphDirection.OUT);
  }

  public static <Identifier> GraphCriteria<Identifier> incoming(Identifier identifier) {
    return new DefaultGraphCriteria<Identifier>(
        Optional.of(identifier),
        Optional.empty(),
        1,
        false,
        GraphDirection.IN);
  }

  public static <Identifier> GraphCriteria<Identifier> any(Identifier origin) {
    return new DefaultGraphCriteria<Identifier>(
        Optional.of(origin),
        Optional.empty(),
        1,
        false,
        GraphDirection.ANY);
  }
}

record DefaultGraphCriteria<Identifier>(
    Optional<Identifier> identifier,
    Optional<Duration> duration,
    int originRadius,
    boolean originIncluded,
    GraphDirection direction) implements GraphCriteria<Identifier> {

  public DefaultGraphCriteria {
    if (originRadius < 1) {
      throw new IllegalArgumentException("originRadius must be positive");
    }
  }
}
