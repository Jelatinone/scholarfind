package com.github.jelatinone.acquisition.interpreter;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;

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
public class PdfInterpreter implements Interpreter<PDPageTree> {

	static int PREVIEW_LENGTH = 500;

	@Override
	public boolean supports(@NonNull MediaType mediaType) {
		return mediaType == MediaType.APPLICATION_PDF;
	}

	@Override
	public Interpreted<PDPageTree> interpret(@NonNull Acquisition.Interpreted acquisition) {
		try (PDDocument document = Loader.loadPDF(acquisition.sourceBytes())) {
			PDPageTree interpretedSource = document.getPages();

			Projection.Interpreted<PDPageTree> projection = new Projection.Interpreted<>(
					acquisition.mediaType(),
					interpretedSource);
			return projection;
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	@Override
	public Normalized normalize(@NonNull Acquisition.Interpreted acquisition) {
		try (PDDocument document = Loader.loadPDF(acquisition.sourceBytes())) {
			String normalizedSource = new PDFTextStripper().getText(document);

			Projection.Normalized projection = new Projection.Normalized(
					acquisition.mediaType(),
					normalizedSource);
			return projection;
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	@Override
	public Preview preview(@NonNull Acquisition.Interpreted acquisition) {
		try (PDDocument document = Loader.loadPDF(acquisition.sourceBytes())) {
			String normalizedSource = Interpreter.bound(new PDFTextStripper().getText(document), PREVIEW_LENGTH);

			Projection.Preview projection = new Projection.Preview(
					acquisition.mediaType(),
					normalizedSource,
					normalizedSource.length(),
					acquisition.mediaMetadata().contentLength());
			return projection;
		} catch (IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

}
