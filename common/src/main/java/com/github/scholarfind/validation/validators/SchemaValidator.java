package com.github.scholarfind.validation.validators;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.validation.*;

public class SchemaValidator<D extends Document<?>>
    implements Validator<D, ValidationContext<D>> {

  private final long expectedVersion;

  public SchemaValidator(long expectedVersion) {
    this.expectedVersion = expectedVersion;
  }

  @Override
  public void validate(ValidatorResult record, ValidationContext<D> context) {
    if (context.document().header().schemaVersion() != expectedVersion) {
      record.fail(Reason.SCHEMA_MISMATCH);
    }
  }
}