package com.github.scholarfind.validation;

import com.github.scholarfind.models.Document;

@FunctionalInterface
public interface Validator<D extends Document<?>, C extends ValidationContext<D>> {

  void validate(ValidatorResult record, C context);

}
