package com.github.scholarfind.validation;

import com.github.scholarfind.models.shared.StageDocument;

@FunctionalInterface
public interface Validator<D extends StageDocument<D>, C extends ValidationContext<D>> {

  void validate(ValidatorResult record, C context);

}
