package com.github.scholarfind.validation;

import com.github.scholarfind.models.shared.StageDocument;

public record ValidatorPipelineResult<Validates extends StageDocument<Validates>>(
    Validates document,
    ValidatorResult result) {

}
