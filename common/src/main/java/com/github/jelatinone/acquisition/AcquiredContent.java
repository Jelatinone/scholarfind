package com.github.jelatinone.acquisition;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.github.jelatinone.models.content.Capture;
import com.github.jelatinone.models.content.CharacterEncoding;
import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.content.ContentKind;
import com.github.jelatinone.models.content.ContentMediaType;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.TargetReference;
import com.github.jelatinone.models.shared.TraceReference;

public record AcquiredContent(
    RequestHeader requestHeader,
    TargetReference target,
    TraceReference trace,
    ContentDocument contentDocument,
    byte[] hydratedSource,
    String hydratedText,
    boolean completeText) {

  public Capture capture() {
    return contentDocument == null
        ? null
        : contentDocument.capture();
  }

  public URL targetUrl() {
    return trace != null && trace.normalizedUrl() != null
        ? trace.normalizedUrl()
        : effectiveUrl();
  }

  public URL traceUrl() {
    return trace == null
        ? null
        : trace.normalizedUrl();
  }

  public URL effectiveUrl() {
    return capture() == null
        ? null
        : capture().effectiveUrl();
  }

  public String traceHost() {
    URL url = traceUrl();
    return url == null
        ? null
        : url.getHost();
  }

  public boolean hasMetadata() {
    return capture() != null
        && capture().effectiveUrl() != null
        && capture().statusCode() != null
        && capture().redirectHopCount() != null
        && capture().setCookieCount() != null
        && capture().contentKind() != null
        && capture().mediaType() != null
        && capture().characterEncoding() != null;
  }

  public boolean hasHydratedSource() {
    return hydratedSource != null
        && hydratedSource.length > 0;
  }

  public boolean hasSourceSnapshot() {
    return capture() != null
        && capture().sourceSnapshot() != null;
  }

  public boolean hasHydratedText() {
    return hydratedText != null
        && !hydratedText.isBlank();
  }

  public boolean hasTextSnapshot() {
    return capture() != null
        && capture().textSnapshot() != null;
  }

  public boolean hasCompleteText() {
    return hasHydratedText()
        && completeText;
  }

  public String traceQuery() {
    URL url = traceUrl();
    return url == null
        ? null
        : url.getQuery();
  }

  public Integer statusCode() {
    return capture() == null
        ? null
        : capture().statusCode();
  }

  public Integer redirectHopCount() {
    return capture() == null
        ? null
        : capture().redirectHopCount();
  }

  public Integer setCookieCount() {
    return capture() == null
        ? null
        : capture().setCookieCount();
  }

  public ContentKind contentKind() {
    return capture() == null
        ? null
        : capture().contentKind();
  }

  public ContentMediaType mediaType() {
    return capture() == null
        ? null
        : capture().mediaType();
  }

  public CharacterEncoding characterEncoding() {
    return capture() == null
        ? null
        : capture().characterEncoding();
  }

  public Long contentLength() {
    return capture() == null
        ? null
        : capture().contentLength();
  }

  public String previewText() {
    return capture() == null
        ? null
        : capture().previewText();
  }

  public boolean hasText() {
    return hasHydratedText()
        || hasTextSnapshot()
        || previewText() != null && !previewText().isBlank();
  }

  public Optional<String> decodedSource() {
    if (!hasHydratedSource()) {
      return Optional.empty();
    }
    return Optional.of(new String(hydratedSource, resolveCharset()));
  }

  private Charset resolveCharset() {
    CharacterEncoding encoding = characterEncoding();
    if (encoding == null || encoding.canonicalName() == null) {
      return StandardCharsets.UTF_8;
    }
    try {
      return Charset.forName(encoding.canonicalName());
    } catch (Exception exception) {
      return StandardCharsets.UTF_8;
    }
  }

  public AcquiredContent withContentDocument(ContentDocument nextDocument) {
    return new AcquiredContent(requestHeader, target, trace, nextDocument, hydratedSource, hydratedText, completeText);
  }

  public AcquiredContent withHydratedSource(byte[] nextSource) {
    return new AcquiredContent(requestHeader, target, trace, contentDocument, nextSource, hydratedText, completeText);
  }

  public AcquiredContent withHydratedText(String nextText, boolean nextCompleteText) {
    return new AcquiredContent(requestHeader, target, trace, contentDocument, hydratedSource, nextText,
        nextCompleteText);
  }
}
