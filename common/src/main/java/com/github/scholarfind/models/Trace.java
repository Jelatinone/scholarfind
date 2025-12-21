package com.github.scholarfind.models;

import java.net.URL;
import java.time.ZonedDateTime;

public record Trace(
        URL url,
        URL parentUrl,

        String reviewer,

        DecisionType decision,

        Integer depth,
        Integer attempt,

        ZonedDateTime discoveredAt,
        ZonedDateTime reviewedAt) {

}
