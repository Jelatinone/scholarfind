package com.github.jelatinone.policy;

import java.time.Instant;

import com.github.jelatinone.model.struct.Document;

public interface PolicyContext<Documents extends Document<Documents>> {

	Documents document();

	Instant reviewedAt();
}
