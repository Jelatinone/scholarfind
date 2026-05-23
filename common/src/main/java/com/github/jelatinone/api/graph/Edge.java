package com.github.jelatinone.api.graph;

import com.github.jelatinone.model.struct.Identity;

import lombok.NonNull;

public interface Edge<Identifier extends Identity, From extends Identity, To extends Identity> {

  @NonNull
  String edgeLabel();

  @NonNull
  Identifier edgeId();

  @NonNull
  From from();

  @NonNull
  To to();
}
