package com.github.scholarfind.validation;

import com.github.scholarfind.models.Document;

public record ValidatorPipelineResult<Validates extends Document<?>>(
    Validates document,
    ValidatorResult result) {

}
