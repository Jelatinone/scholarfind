package com.github.jelatinone.model.content;

import java.util.Locale;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
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

	public static MediaEncoding resolve(String header) {
		if (header == null || header.isBlank()) {
			return MediaEncoding.UTF_8;
		}
		String[] parts = header.split(";");
		for (String part : parts) {
			String normalized = part.trim().toLowerCase(Locale.ROOT);
			if (!normalized.startsWith("charset=")) {
				continue;
			}
			String value = normalized.substring("charset=".length()).replace("\"", "");
			return switch (value) {
				case "utf-8" ->
					MediaEncoding.UTF_8;
				case "utf-16" ->
					MediaEncoding.UTF_16;
				case "utf-16le" ->
					MediaEncoding.UTF_16LE;
				case "utf-16be" ->
					MediaEncoding.UTF_16BE;
				case "us-ascii" ->
					MediaEncoding.US_ASCII;
				case "iso-8859-1" ->
					MediaEncoding.ISO_8859_1;
				case "windows-1252" ->
					MediaEncoding.WINDOWS_1252;
				default -> MediaEncoding.OTHER;
			};
		}
		return MediaEncoding.UTF_8;
	}
}
