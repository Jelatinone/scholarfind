package com.github.scholarfind.models;

import java.util.UUID;

import lombok.NonNull;

public record Header(
    long schemaVersion,
    @NonNull UUID id,
    @NonNull Lifecycle state) {

}
