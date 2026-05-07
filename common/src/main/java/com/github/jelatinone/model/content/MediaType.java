package com.github.jelatinone.model.content;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
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

	public static MediaType resolve(String header) {
		if (header == null || header.isBlank()) {
			return MediaType.OTHER;
		}
		String normalized = header.split(";")[0].trim().toLowerCase();
		if (normalized.contains("html")) {
			return MediaType.TEXT_HTML;
		}
		return switch (normalized) {
			case "application/pdf" ->
				MediaType.APPLICATION_PDF;
			case "text/plain" ->
				MediaType.TEXT_PLAIN;
			case "text/markdown" ->
				MediaType.TEXT_MARKDOWN;
			case "application/xml" ->
				MediaType.APPLICATION_XML;
			case "text/xml" ->
				MediaType.TEXT_XML;
			case "application/json" ->
				MediaType.APPLICATION_JSON;
			case "text/json" ->
				MediaType.TEXT_JSON;
			case "application/octet-stream" ->
				MediaType.APPLICATION_OCTET_STREAM;
			default -> MediaType.OTHER;
		};
	}
}
