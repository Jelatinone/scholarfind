package com.github.scholarfind.validation;

import com.github.scholarfind.models.Document;

public interface ValidationContext<Validates extends Document<?>> {

  Validates document();

}
