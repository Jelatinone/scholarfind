package com.github.jelatinone.api.graph;

import lombok.NonNull;

public interface Vertex<Identifier> {

  @NonNull
  String vertexLabel();

  @NonNull
  Identifier vertexId();
}
