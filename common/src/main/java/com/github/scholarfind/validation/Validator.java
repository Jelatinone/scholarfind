package com.github.scholarfind.validation;

import com.github.scholarfind.models.Document;
import java.lang.FunctionalInterface;

@FunctionalInterface
public interface Validator<Validates extends Document<?>> {

  void validate(ValidatorResult record, ValidationContext<Validates> context);

}
