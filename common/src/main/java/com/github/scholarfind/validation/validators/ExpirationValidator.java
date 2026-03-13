package com.github.scholarfind.validation.validators;

import java.time.Instant;

import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.validation.*;

public class ExpirationValidator<D extends StageDocument<D>>
    implements Validator<D, ValidationContext<D>> {

  private final int expirationDays;

  public ExpirationValidator(int expirationDays) {
    this.expirationDays = expirationDays;
  }

  @Override
  public void validate(ValidatorResult record, ValidationContext<D> context) {
    Instant discoveredAt = context.document().target().discoveredAt();
    Instant now = Instant.now();

    if (discoveredAt != null && discoveredAt.plusSeconds((long) expirationDays * 24 * 60 * 60).isBefore(now)) {
      record.fail(Reason.DOCUMENT_EXPIRED);
    }
  }
}
