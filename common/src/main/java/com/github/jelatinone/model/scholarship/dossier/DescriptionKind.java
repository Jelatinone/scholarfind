package com.github.jelatinone.model.scholarship.dossier;

import java.net.URL;

import com.github.jelatinone.model.scholarship.dossier.description.Award;
import com.github.jelatinone.model.scholarship.dossier.description.Window;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum DescriptionKind {

	SCHOLARSHIP_NAME(String.class),
	ORGANIZATION_NAME(String.class),

	CANONICAL_URL(URL.class),
	APPLICATION_URL(URL.class),

	AWARD(Award.class),

	APPLICATION_WINDOW(Window.class),

	OTHER_TEXT(String.class);

	Class<?> descriptionType;

	public boolean accepts(Object description) {
		return descriptionType.isInstance(description);
	}
}
