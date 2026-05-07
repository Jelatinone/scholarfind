package com.github.jelatinone.acquisition.interpreter;

import com.github.jelatinone.acquisition.Acquisition;
import com.github.jelatinone.acquisition.Interpreter;
import com.github.jelatinone.acquisition.Projection;
import com.github.jelatinone.acquisition.Projection.Interpreted;
import com.github.jelatinone.acquisition.Projection.Normalized;
import com.github.jelatinone.acquisition.Projection.Preview;
import com.github.jelatinone.model.content.MediaType;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TextInterpreter implements Interpreter<String> {

	static int PREVIEW_LENGTH = 500;

	@Override
	public boolean supports(@NonNull MediaType mediaType) {
		return switch (mediaType) {
			case TEXT_PLAIN, TEXT_MARKDOWN,
					APPLICATION_XML, TEXT_XML,
					APPLICATION_JSON, TEXT_JSON ->
				true;
			default -> false;
		};
	}

	@Override
	public Interpreted<String> interpret(@NonNull Acquisition.Interpreted acquisition) {
		String decodedSource = Interpreter.decode(acquisition.sourceBytes(), acquisition.mediaEncoding());

		Projection.Interpreted<String> projection = new Projection.Interpreted<>(
				acquisition.mediaType(),
				decodedSource);
		return projection;
	}

	@Override
	public Normalized normalize(@NonNull Acquisition.Interpreted acquisition) {
		String decodedSource = Interpreter.decode(acquisition.sourceBytes(), acquisition.mediaEncoding());
		String normalizedSource = decodedSource.trim();

		Projection.Normalized projection = new Projection.Normalized(
				acquisition.mediaType(),
				normalizedSource);
		return projection;
	}

	@Override
	public Preview preview(@NonNull Acquisition.Interpreted acquisition) {
		String decodedSource = Interpreter.decode(acquisition.sourceBytes(), acquisition.mediaEncoding());
		String previewedSource = Interpreter.bound(decodedSource, PREVIEW_LENGTH);

		Projection.Preview projection = new Projection.Preview(
				acquisition.mediaType(),
				previewedSource,
				previewedSource.length(),
				acquisition.mediaMetadata().contentLength());
		return projection;
	}

}
