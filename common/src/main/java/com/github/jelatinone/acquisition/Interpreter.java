package com.github.jelatinone.acquisition;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;

import lombok.NonNull;

public interface Interpreter<Interprets> {

	static String decode(byte[] source, @NonNull MediaEncoding encoding) {
		Charset charset = StandardCharsets.UTF_8;
		if (encoding.getCanonicalName() != null) {
			try {
				charset = Charset.forName(encoding.getCanonicalName());
			} catch (Exception ignored) {
			}
		}
		return new String(source, charset);
	}

	static String bound(@NonNull String text, int previewLength) {
		if (text.isBlank()) {
			return text;
		}
		String normalized = text.trim();
		return normalized.length() <= previewLength
				? normalized
				: normalized.substring(0, previewLength);
	}

	boolean supports(@NonNull MediaType mediaType);

	Projection.Interpreted<Interprets> interpret(@NonNull Acquisition.Interpreted acquisition);

	Projection.Normalized normalize(@NonNull Acquisition.Interpreted acquisition);

	Projection.Preview preview(@NonNull Acquisition.Interpreted acquisition);
}
