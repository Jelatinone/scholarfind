package com.github.jelatinone.model.archive.scholarship.dossier;

import lombok.NonNull;

public record Description<Describes>(
		@NonNull DescriptionKind descriptionKind,
		@NonNull Describes description,

		String descriptor) {

	public Description {
		if (!descriptionKind.accepts(description)) {
			throw new IllegalArgumentException(
					String.format(("Description of kind %s cannot be bounded by value %s."),
							descriptionKind,
							description().getClass().getSimpleName()));
		}
	}
}
