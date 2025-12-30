package com.github.scholarfind.validation;

import com.github.scholarfind.models.Document;

public interface Validator<Validates extends Document> {

  void validate(Validates validates, ValidatorResult record);

}
