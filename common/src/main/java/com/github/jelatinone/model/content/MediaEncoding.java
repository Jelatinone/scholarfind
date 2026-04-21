package com.github.jelatinone.model.content;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum MediaEncoding {
	UTF_8("UTF-8"),
	UTF_16("UTF-16"),
	UTF_16LE("UTF-16LE"),
	UTF_16BE("UTF-16BE"),
	US_ASCII("US-ASCII"),
	ISO_8859_1("ISO-8859-1"),
	WINDOWS_1252("windows-1252"),
	OTHER(null);

	@Getter
	String canonicalName;

	MediaEncoding(@NonNull final String canonicalName) {
		this.canonicalName = canonicalName;
	}
}
