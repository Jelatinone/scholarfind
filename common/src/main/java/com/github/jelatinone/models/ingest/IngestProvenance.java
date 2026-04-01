package com.github.jelatinone.models.ingest;

import java.util.UUID;

public record IngestProvenance(
        IngestOrigin origin,
        String providerKey,
        UUID sourceDocumentId,
        UUID sourceTargetId) {
}
