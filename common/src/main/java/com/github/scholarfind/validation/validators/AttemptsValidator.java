package com.github.scholarfind.validation.validators;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.validation.*;

public class AttemptsValidator<D extends Document<?>>
    implements Validator<D, ValidationContext<D>> {

  private final int maxAttempts;

  public AttemptsValidator(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  @Override
  public void validate(ValidatorResult record, ValidationContext<D> context) {
    if (context.document().header().attempt() >= maxAttempts) {
      record.fail(Reason.ATTEMPTS_EXCEEDED);
    }
  }
}