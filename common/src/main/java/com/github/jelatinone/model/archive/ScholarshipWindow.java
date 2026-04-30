package com.github.jelatinone.model.archive;

import java.time.LocalDate;

public record ScholarshipWindow(
		LocalDate openDate,
		LocalDate closeDate,
		String descriptor) {
}
