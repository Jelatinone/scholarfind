package com.github.scholarfind.models;

import java.time.ZonedDateTime;

public record Timestamp(
    ZonedDateTime discoveredAt,
    ZonedDateTime reviewedAt) {

}
