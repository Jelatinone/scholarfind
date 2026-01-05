package com.github.scholarfind.validation;

import java.util.Set;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.utility.Builder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidatorPipeline<Validates extends Document<Validates>, Context extends ValidationContext<Validates>> {

  Set<Validator<Validates>> _validators;
  Set<Mutator<Validates, Context>> _mutations;

  public ValidatorPipelineResult<Validates> process(Context context) {
    ValidatorResult record = new ValidatorResult();
    _validators.forEach((validator) -> validator.validate(record, context));

    if (!record.processable()) {
      return new ValidatorPipelineResult<>(context.document(), record);
    }

    Builder<Validates> builder = context.document().toBuilder();
    _mutations.stream()
        .filter((mutator) -> record.capabilities().containsAll(mutator.capabilities()))
        .forEach((mutator) -> mutator.mutate(builder, context));

    return new ValidatorPipelineResult<>(builder.build(), record);
  }
}
