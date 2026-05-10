package com.github.jelatinone.infra.aws.store;

public record StoreLocation<Value>(String tableName, Value value) {
}
