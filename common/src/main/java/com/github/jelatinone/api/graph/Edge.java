package com.github.jelatinone.api.graph;

import lombok.NonNull;

public interface Edge<Identifier> {

  @NonNull
  String edgeLabel();

  @NonNull
  Identifier edgeId();

  @NonNull
  Identifier from();

  @NonNull
  Identifier to();
}
