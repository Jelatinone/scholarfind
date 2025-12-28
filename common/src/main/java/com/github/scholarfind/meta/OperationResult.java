package com.github.scholarfind.meta;

import com.github.scholarfind.models.DecisionType;

public record OperationResult<T>(
                T value,
                DecisionType decision) {

}
