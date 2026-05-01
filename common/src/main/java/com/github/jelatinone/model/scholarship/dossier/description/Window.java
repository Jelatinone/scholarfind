package com.github.jelatinone.model.scholarship.dossier.description;

import java.time.LocalDate;

import lombok.NonNull;

public record Window(
		@NonNull LocalDate openDate,
		@NonNull LocalDate closeDate,

		String descriptor) {
}
