package com.github.jelatinone.api.graph;

import java.time.Duration;
import java.util.Optional;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.model.struct.Identity;

public interface GraphCriteria<Identifier extends Identity> extends Criteria<Identifier> {

  int originRadius();

  boolean originIncluded();

  GraphDirection direction();

  public static <Identifier extends Identity> GraphCriteria<Identifier> outgoing(Identifier identifier) {
    return new DefaultGraphCriteria<Identifier>(
        Optional.of(identifier),
        Optional.empty(),
        1,
        false,
        GraphDirection.OUT);
  }

  public static <Identifier extends Identity> GraphCriteria<Identifier> incoming(Identifier identifier) {
    return new DefaultGraphCriteria<Identifier>(
        Optional.of(identifier),
        Optional.empty(),
        1,
        false,
        GraphDirection.IN);
  }

  public static <Identifier extends Identity> GraphCriteria<Identifier> any(Identifier origin) {
    return new DefaultGraphCriteria<Identifier>(
        Optional.of(origin),
        Optional.empty(),
        1,
        false,
        GraphDirection.ANY);
  }
}

record DefaultGraphCriteria<Identifier extends Identity>(
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
