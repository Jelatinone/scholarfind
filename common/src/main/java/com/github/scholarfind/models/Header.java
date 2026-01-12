package com.github.scholarfind.models;

import java.util.UUID;

import lombok.NonNull;

public record Header(
        long schemaVersion,
        int attempt,
        @NonNull UUID id,
        @NonNull Lifecycle state) {

}
