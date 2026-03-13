package com.github.scholarfind.validation.mutators;

import java.util.Set;

import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.validation.Capability;
import com.github.scholarfind.validation.Mutator;
import com.github.scholarfind.validation.ValidationContext;

public class AttemptMutator<D extends StageDocument<D>>
    implements Mutator<D, ValidationContext<D>> {

  @Override
  public Set<Capability> capabilities() {
    return Set.of();
  }

  @Override
  public D mutate(D document, ValidationContext<D> context) {
    RequestHeader header = document.requestHeader();
    return document.withRequestHeader(
        new RequestHeader(
            header.schemaVersion(),
            header.requestId(),
            header.attempt() + 1,
            header.idempotencyKey(),
            header.enqueuedAt()));
  }
}
