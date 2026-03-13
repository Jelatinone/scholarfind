package com.github.scholarfind.validation.validators;

import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.validation.*;

public class AttemptsValidator<D extends StageDocument<D>>
    implements Validator<D, ValidationContext<D>> {

  private final int maxAttempts;

  public AttemptsValidator(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  @Override
  public void validate(ValidatorResult record, ValidationContext<D> context) {
    if (context.document().requestHeader().attempt() >= maxAttempts) {
      record.fail(Reason.ATTEMPTS_EXCEEDED);
    }
  }
}
