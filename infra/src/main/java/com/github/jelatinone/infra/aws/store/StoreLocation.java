package com.github.jelatinone.infra.aws.store;

import com.github.jelatinone.model.struct.Identity;

public record StoreLocation<Key extends Identity>(String tableName, Key value) {
}
