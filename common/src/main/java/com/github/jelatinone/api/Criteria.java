package com.github.jelatinone.api;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface Criteria<Identifier> {

  Optional<Identifier> identifier();

  Optional<Duration> duration();

  Map<Index<?>, Property> queryProperties();

  static <Identifier> Criteria<Identifier> identifier(Identifier id) {
    return new Implicit<>(Optional.of(id), Optional.empty(), Map.of());
  }

  static <Identifier> Criteria<Identifier> property(Index<?> key, Property value) {
    return new Implicit<Identifier>(Optional.empty(), Optional.empty(), Map.of(key, value));
  }

  static <Identifier> Criteria<Identifier> duration(Duration duration) {
    return new Implicit<Identifier>(Optional.empty(), Optional.of(duration), Map.of());
  }

  default Criteria<Identifier> withProperty(Index<?> key, Property value) {
    Map<Index<?>, Property> next = new HashMap<>(queryProperties()) {
      {
        put(key, value);
      }
    };
    return new Implicit<Identifier>(identifier(), duration(), Map.copyOf(next));
  }
}

record Implicit<Identifier>(
    Optional<Identifier> identifier,
    Optional<Duration> duration,
    Map<Index<?>, Property> queryProperties) implements Criteria<Identifier> {
}
