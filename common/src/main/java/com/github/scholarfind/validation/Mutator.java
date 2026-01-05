package com.github.scholarfind.validation;

import java.util.Set;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.utility.Builder;

public interface Mutator<Mutates extends Document<?>, Context extends ValidationContext<Mutates>> {

  Set<Capability> capabilities();

  void mutate(Builder<Mutates> builder, Context context);

}
