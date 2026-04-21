package com.github.jelatinone.model.content;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum MediaType {
	TEXT_HTML("text/html"),
	APPLICATION_PDF("application/pdf"),
	TEXT_PLAIN("text/plain"),
	TEXT_MARKDOWN("text/markdown"),
	APPLICATION_XML("application/xml"),
	TEXT_XML("text/xml"),
	APPLICATION_JSON("application/json"),
	TEXT_JSON("text/json"),
	APPLICATION_OCTET_STREAM("application/octet-stream"),
	OTHER(null);

	@Getter
	String canonicalName;

	MediaType(@NonNull final String canonicalName) {
		this.canonicalName = canonicalName;
	}
}
