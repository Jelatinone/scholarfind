package com.github.scholarfind.validation.validators;

import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.validation.*;

public class SchemaValidator<D extends StageDocument<D>>
    implements Validator<D, ValidationContext<D>> {

  private final long expectedVersion;

  public SchemaValidator(long expectedVersion) {
    this.expectedVersion = expectedVersion;
  }

  @Override
  public void validate(ValidatorResult record, ValidationContext<D> context) {
    if (context.document().documentHeader().schemaVersion() != expectedVersion) {
      record.fail(Reason.SCHEMA_MISMATCH);
    }
  }
}
