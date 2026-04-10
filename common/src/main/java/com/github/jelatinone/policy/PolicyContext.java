package com.github.jelatinone.policy;

import java.time.Instant;

import com.github.jelatinone.models.shared.StageDocument;

public interface PolicyContext<Document extends StageDocument<Document>> {

	Document document();

	Instant reviewedAt();
}
