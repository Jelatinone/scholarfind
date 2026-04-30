package com.github.jelatinone.model.archive.scholarship.dossier.description;

import java.time.LocalDate;

public record Window(
		LocalDate openDate,
		LocalDate closeDate) {
}
