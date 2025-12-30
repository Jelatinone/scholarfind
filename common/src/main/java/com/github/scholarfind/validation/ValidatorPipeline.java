package com.github.scholarfind.validation;

import java.util.Set;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.utility.Builder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidatorPipeline<Validates extends Document, Context> {

  Set<Validator<Validates>> _validators;
  Set<Mutator<Validates, Context>> _mutations;

  public Validates process(Validates document, Builder<Validates> builder, Context context) {
    ValidatorResult record = new ValidatorResult();
    _validators.forEach((validator) -> validator.validate(document, record));

    _mutations.stream()
        .filter((mutator) -> record.capabilities().containsAll(mutator.capabilities()))
        .forEach((mutator) -> mutator.mutate(builder, context));

    return builder.build();
  }
}
