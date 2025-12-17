package com.github.scholarfind.models.document;

import java.net.URL;
import java.time.chrono.ChronoLocalDate;
import java.util.UUID;

public record HeaderDocument(
        UUID id,

        URL url,
        URL parentUrl,

        Integer depth,

        ChronoLocalDate discoveredAt,
        ChronoLocalDate reviewedAt) {

}
