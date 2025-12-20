package com.github.scholarfind.models.document;

import java.net.URL;
import java.time.chrono.ChronoLocalDate;

public record Trace(
                URL url,
                URL parentUrl,

                String reviewer,

                DecisionType decision,

                Integer depth,
                Integer attempt,

                ChronoLocalDate discoveredAt,
                ChronoLocalDate reviewedAt) {

}
