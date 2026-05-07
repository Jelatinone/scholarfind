package com.github.jelatinone.acquisition.interpreter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
public class HtmlInterpreter implements Interpreter<Document> {

	static int PREVIEW_BOUND = 500;

	@Override
	public boolean supports(@NonNull MediaType mediaType) {
		return mediaType == MediaType.TEXT_HTML;
	}

	@Override
	public Interpreted<Document> interpret(@NonNull Acquisition.Interpreted acquisition) {
		String decodedSource = Interpreter.decode(acquisition.sourceBytes(), acquisition.mediaEncoding());
		Document interpretedSource = Jsoup.parse(decodedSource);

		Projection.Interpreted<Document> projection = new Projection.Interpreted<>(
				acquisition.mediaType(),
				interpretedSource);
		return projection;
	}

	@Override
	public Normalized normalize(@NonNull Acquisition.Interpreted acquisition) {
		String decodedSource = Interpreter.decode(acquisition.sourceBytes(), acquisition.mediaEncoding());
		String normalizedSource = Jsoup.parse(decodedSource).text();

		Projection.Normalized projection = new Projection.Normalized(
				acquisition.mediaType(),
				normalizedSource);
		return projection;
	}

	@Override
	public Preview preview(@NonNull Acquisition.Interpreted acquisition) {
		String decodedSource = Interpreter.decode(acquisition.sourceBytes(), acquisition.mediaEncoding());
		String previewedSource = Interpreter.bound(Jsoup.parse(decodedSource).text(), PREVIEW_BOUND);

		Projection.Preview projection = new Projection.Preview(
				acquisition.mediaType(),
				previewedSource,
				previewedSource.length(),
				acquisition.mediaMetadata().contentLength());
		return projection;
	}

}
