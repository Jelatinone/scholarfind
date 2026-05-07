package com.github.jelatinone.acquisition.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import com.github.jelatinone.acquisition.Projection;
import com.github.jelatinone.fixtures.AcquisitionTestFixtures;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaType;

class InterpreterContractsTest {

	@Test
	void textInterpreter_supportsConfiguredTextTypes_andBuildsPreview() {
		TextInterpreter interpreter = new TextInterpreter();
		var acquisition = AcquisitionTestFixtures.acquisition(
				"  hello world  ".getBytes(),
				MediaType.TEXT_PLAIN,
				MediaEncoding.UTF_8,
				AcquisitionTestFixtures.mediaMetadata("text/plain", 15));

		assertTrue(interpreter.supports(MediaType.TEXT_PLAIN));
		assertFalse(interpreter.supports(MediaType.APPLICATION_PDF));
		assertEquals("  hello world  ", interpreter.interpret(acquisition).interpretedSource());
		assertEquals("hello world", interpreter.normalize(acquisition).normalizedSource());
		assertEquals("hello world", interpreter.preview(acquisition).previewSource());
	}

	@Test
	void htmlInterpreter_parsesDocumentAndText() {
		HtmlInterpreter interpreter = new HtmlInterpreter();
		var acquisition = AcquisitionTestFixtures.acquisition(
				"<html><body><h1>Hello</h1><p>World</p></body></html>".getBytes(),
				MediaType.TEXT_HTML,
				MediaEncoding.UTF_8,
				AcquisitionTestFixtures.mediaMetadata("text/html", 52));

		Projection.Interpreted<org.jsoup.nodes.Document> interpreted = interpreter.interpret(acquisition);

		assertTrue(interpreter.supports(MediaType.TEXT_HTML));
		assertEquals("HelloWorld", interpreted.interpretedSource().body().text().replace(" ", ""));
		assertEquals("Hello World", interpreter.normalize(acquisition).normalizedSource());
		assertEquals(52L, interpreter.preview(acquisition).sourceLength());
	}

	@Test
	void pdfInterpreter_extractsTextWithoutLeakingClosedDocumentState() throws Exception {
		PdfInterpreter interpreter = new PdfInterpreter();
		byte[] pdfBytes = createPdf("Hello PDF");
		var acquisition = AcquisitionTestFixtures.acquisition(
				pdfBytes,
				MediaType.APPLICATION_PDF,
				MediaEncoding.UTF_8,
				AcquisitionTestFixtures.mediaMetadata("application/pdf", pdfBytes.length));

		assertTrue(interpreter.supports(MediaType.APPLICATION_PDF));
		assertFalse(interpreter.supports(MediaType.TEXT_HTML));
		assertTrue(interpreter.interpret(acquisition).interpretedSource().contains("Hello PDF"));
		assertTrue(interpreter.normalize(acquisition).normalizedSource().contains("Hello PDF"));
		assertTrue(interpreter.preview(acquisition).previewSource().contains("Hello PDF"));
	}

	private static byte[] createPdf(String text) throws Exception {
		Path cacheDir = Path.of("build", "pdfbox-font-cache");
		Files.createDirectories(cacheDir);
		System.setProperty("pdfbox.fontcache", cacheDir.toAbsolutePath().toString());

		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);

			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 700);
				stream.showText(text);
				stream.endText();
			}

			document.save(output);
			return output.toByteArray();
		}
	}
}
