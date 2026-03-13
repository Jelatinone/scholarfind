package com.github.scholarfind.validation;

import java.util.Set;

import com.github.scholarfind.models.shared.StageDocument;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ValidatorPipeline<D extends StageDocument<D>, C extends ValidationContext<D>> {

  Set<Validator<D, ? super C>> _validators;
  Set<Mutator<D, C>> _mutators;

  public ValidatorPipelineResult<D> process(C context) {
    ValidatorResult record = new ValidatorResult();

    _validators.forEach(validator -> validator.validate(record, context));

    if (!record.processable()) {
      return new ValidatorPipelineResult<>(context.document(), record);
    }

    final var mutatedDocument = new java.util.concurrent.atomic.AtomicReference<D>(context.document());
    _mutators.stream()
        .filter(mutator -> record.capabilities().containsAll(mutator.capabilities()))
        .forEach(mutator -> mutatedDocument.set(mutator.mutate(mutatedDocument.get(), context)));

    return new ValidatorPipelineResult<>(mutatedDocument.get(), record);
  }
}
