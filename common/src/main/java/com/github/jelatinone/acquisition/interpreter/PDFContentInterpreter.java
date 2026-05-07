package com.github.jelatinone.acquisition.interpreter;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.github.jelatinone.acquisition.ContentInterpreter;
import com.github.jelatinone.acquisition.DetectedContent;
import com.github.jelatinone.acquisition.InterpretedContent;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PDFContentInterpreter implements ContentInterpreter {

  static int PREVIEW_LIMIT = 500;

  @Override
  public boolean supports(@NonNull DetectedContent contentType) {
    return contentType.pdf();
  }

  @Override
  public @NonNull InterpretedContent interpret(byte[] source,
      @NonNull DetectedContent contentType) {

    try (PDDocument document = Loader.loadPDF(source)) {
      document.getPages().get(0);
      String normalizedSource = new PDFTextStripper().getText(document);

      return new InterpretedContent(
          source,
          normalizedSource,
          ContentInterpreter.preview(normalizedSource, PREVIEW_LIMIT));
    } catch (IOException exception) {
      return new InterpretedContent(source, null, null);
    }
  }

}
