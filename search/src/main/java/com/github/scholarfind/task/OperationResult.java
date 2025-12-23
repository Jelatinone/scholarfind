package com.github.scholarfind.task;

import com.github.scholarfind.models.DecisionType;

public record OperationResult<T>(
    T document,
    DecisionType decision) {

}
