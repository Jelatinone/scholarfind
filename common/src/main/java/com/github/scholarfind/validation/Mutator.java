package com.github.scholarfind.validation;

import java.util.Set;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.utility.Builder;

public interface Mutator<D extends Document<?>, Context extends ValidationContext<?>> {

  Set<Capability> capabilities();

  <Builds extends Builder<D>> void mutate(Builds builder, Context context);

}
