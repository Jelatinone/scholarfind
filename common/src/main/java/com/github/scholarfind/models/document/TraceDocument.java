package com.github.scholarfind.models.document;

import java.net.URL;
import java.time.chrono.ChronoLocalDate;

public record TraceDocument(
    URL url,
    URL parentUrl,

    String origin,

    Integer depth,
    Integer attempt,

    ChronoLocalDate discoveredAt,
    ChronoLocalDate reviewedAt) {

}
