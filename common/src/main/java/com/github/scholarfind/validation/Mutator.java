package com.github.scholarfind.validation;

import java.util.Set;
import com.github.scholarfind.models.Document;
import com.github.scholarfind.utility.Builder;

public interface Mutator<D extends Document<?>, C extends ValidationContext<D>> {

  Set<Capability> capabilities();

  void mutate(Builder<D> builder, C context);

}
