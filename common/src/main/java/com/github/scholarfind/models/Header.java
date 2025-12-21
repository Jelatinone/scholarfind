package com.github.scholarfind.models;

import java.util.UUID;

public record Header(
        Long schemaVersion,
        UUID id) {

}
