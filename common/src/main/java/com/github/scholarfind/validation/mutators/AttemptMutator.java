package com.github.scholarfind.validation.mutators;

import java.util.Set;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.utility.Builder;
import com.github.scholarfind.validation.Capability;
import com.github.scholarfind.validation.Mutator;
import com.github.scholarfind.validation.ValidationContext;

public class AttemptMutator<D extends Document<?>>
    implements Mutator<D, ValidationContext<?>> {

  @Override
  public Set<Capability> capabilities() {
    return Set.of();
  }

  @Override
  public <Builds extends Builder<D>> void mutate(Builds builder, ValidationContext<?> context) {
    Header header = context.document().header();
    builder.setHeader(
        new Header(header.schemaVersion(), header.attempt() + 1, header.id(), header.state()));
  }
}
