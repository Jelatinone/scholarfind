package com.github.scholarfind.models.document;

import java.util.UUID;

public record Header(
    Long schemaVersion,
    UUID id) {

}
