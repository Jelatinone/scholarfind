package com.github.scholarfind.validation;

import java.util.Set;
import com.github.scholarfind.models.Document;
import com.github.scholarfind.utility.Builder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidatorPipeline<D extends Document<D>, C extends ValidationContext<D>> {

  Set<Validator<D, ? super C>> _validators;
  Set<Mutator<D, ? super C>> _mutators;

  public ValidatorPipelineResult<D> process(C context) {
    ValidatorResult record = new ValidatorResult();

    _validators.forEach(validator -> validator.validate(record, context));

    if (!record.processable()) {
      return new ValidatorPipelineResult<>(context.document(), record);
    }

    Builder<D> builder = context.document().toBuilder();
    _mutators.stream()
        .filter(mutator -> record.capabilities().containsAll(mutator.capabilities()))
        .forEach(mutator -> mutator.mutate(builder, context));

    return new ValidatorPipelineResult<>(builder.build(), record);
  }
}