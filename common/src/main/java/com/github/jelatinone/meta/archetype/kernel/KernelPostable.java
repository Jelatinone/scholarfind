package com.github.jelatinone.meta.archetype.kernel;

import java.util.Collection;
import java.util.List;

import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

import lombok.NonNull;

public record KernelPostable<Emit extends Request>(
    @NonNull KernelOperable operable,
    Collection<@NonNull Letter<Emit>> letters) {

  public KernelPostable {
    letters = List.copyOf(letters);
  }
}
