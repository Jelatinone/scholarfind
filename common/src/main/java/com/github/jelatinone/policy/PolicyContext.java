package com.github.jelatinone.policy;

import java.time.Instant;

import com.github.jelatinone.model.struct.Document;

public interface PolicyContext<Doc extends Document<Doc>> {

	Doc document();

	Instant reviewedAt();
}
