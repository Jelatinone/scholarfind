package com.github.jelatinone.model.archive.scholarship.dossier;

public record Requirement<Requires>(
		RequirementKind requirementKind,
		Requires requirement,

		String descriptor,
		Magnitude magnitude) {
}
