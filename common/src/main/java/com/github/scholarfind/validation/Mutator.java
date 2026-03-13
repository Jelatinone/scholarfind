package com.github.scholarfind.validation;

import java.util.Set;

import com.github.scholarfind.models.shared.StageDocument;

public interface Mutator<D extends StageDocument<D>, Context extends ValidationContext<D>> {

  Set<Capability> capabilities();

  D mutate(D document, Context context);

}
