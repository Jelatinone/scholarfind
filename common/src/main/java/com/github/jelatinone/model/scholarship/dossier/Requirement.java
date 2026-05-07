package com.github.jelatinone.model.scholarship.dossier;

import lombok.NonNull;

public record Requirement<Requires>(
		@NonNull RequirementKind requirementKind,
		@NonNull Requires requirement,

		@NonNull Magnitude magnitude,

		String descriptor) {

	public Requirement {
		if (!requirementKind.accepts(requirement)) {
			throw new IllegalArgumentException(
					String.format(("Requirement of kind %s cannot be bounded by value %s."),
							requirementKind,
							requirement.getClass().getSimpleName()));
		}
	}
}
