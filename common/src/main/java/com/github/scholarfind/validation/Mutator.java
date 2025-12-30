package com.github.scholarfind.validation;

import java.security.DrbgParameters.Capability;

import com.github.scholarfind.utility.Builder;

public interface Mutator<Mutable, Context> {

  Capability capability();

  void mutate(Builder<Mutable> builder, Context context);

}
