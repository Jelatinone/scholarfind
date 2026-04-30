package com.github.jelatinone.model.scholarship.dossier.description;

import java.time.LocalDate;

public record Window(
		LocalDate openDate,
		LocalDate closeDate,

		String descriptor) {
}
