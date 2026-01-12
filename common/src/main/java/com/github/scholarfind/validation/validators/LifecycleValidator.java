package com.github.scholarfind.validation.validators;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.models.Lifecycle;
import com.github.scholarfind.validation.Reason;
import com.github.scholarfind.validation.ValidationContext;
import com.github.scholarfind.validation.Validator;
import com.github.scholarfind.validation.ValidatorResult;

public class LifecycleValidator<D extends Document<?>>
    implements Validator<D, ValidationContext<D>> {

  @Override
  public void validate(ValidatorResult record, ValidationContext<D> context) {
    if (context.document().header().state() != Lifecycle.CURRENT) {
      record.fail(Reason.STATE_EXCEEDED);
    }
  }
}
