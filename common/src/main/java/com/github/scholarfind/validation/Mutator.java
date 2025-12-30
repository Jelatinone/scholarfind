package com.github.scholarfind.validation;

import java.util.Set;

import com.github.scholarfind.utility.Builder;

public interface Mutator<Mutable, Context> {

  Set<Capability> capabilities();

  void mutate(Builder<Mutable> builder, Context context);

}
