package com.github.scholarfind.validation;

import com.github.scholarfind.models.shared.StageDocument;

public interface ValidationContext<Validates extends StageDocument<Validates>> {

  Validates document();

}
