package com.github.scholarfind.validation.validators;

import java.time.ZonedDateTime;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.validation.*;

public class ExpirationValidator<D extends Document<?>>
    implements Validator<D, ValidationContext<D>> {

  private final int expirationDays;

  public ExpirationValidator(int expirationDays) {
    this.expirationDays = expirationDays;
  }

  @Override
  public void validate(ValidatorResult record, ValidationContext<D> context) {
    ZonedDateTime discoveredAt = context.document().timestamp().discoveredAt();
    ZonedDateTime now = ZonedDateTime.now();

    if (discoveredAt.plusDays(expirationDays).isBefore(now)) {
      record.fail(Reason.DOCUMENT_EXPIRED);
    }
  }
}