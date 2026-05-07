package com.github.jelatinone.model.scholarship.dossier;

import lombok.NonNull;

public record Description<Describes>(
		@NonNull DescriptionKind descriptionKind,
		@NonNull Describes description) {

	public Description {
		if (!descriptionKind.accepts(description)) {
			throw new IllegalArgumentException(
					String.format(("Description of kind %s cannot be bounded by value %s."),
							descriptionKind,
							description.getClass().getSimpleName()));
		}
	}
}
