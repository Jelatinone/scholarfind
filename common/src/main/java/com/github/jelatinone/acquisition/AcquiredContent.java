package com.github.jelatinone.acquisition;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import com.github.jelatinone.model.content.Capture;
import com.github.jelatinone.model.content.ContentDocument;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.model.graph.TargetNode;
import com.github.jelatinone.model.struct.RequestHeader;

public record AcquiredContent(
    RequestHeader requestHeader,
    TargetNode target,
    UUID reviewId,
    ContentDocument contentDocument,
    byte[] hydratedSource,
    String hydratedText,
    boolean completeText) {

  public Capture capture() {
    return contentDocument == null
        ? null
        : contentDocument.capture();
  }

  public UUID targetId() {
    if (target != null) {
      return target.targetId();
    }
    if (requestHeader != null) {
      return requestHeader.targetId();
    }
    return capture() == null
        ? null
        : capture().targetId();
  }

  public URL targetUrl() {
    if (target != null) {
      return target.canonicalUrl();
    }
    return effectiveUrl();
  }

  public URL traceUrl() {
    return targetUrl();
  }

  public URL effectiveUrl() {
    return capture() == null
        ? null
        : capture().canonicalUrl();
  }

  public String traceHost() {
    URL url = traceUrl();
    return url == null
        ? null
        : url.getHost();
  }

  public boolean hasMetadata() {
    MediaMetadata metadata = mediaMetadata();
    return capture() != null
        && capture().canonicalUrl() != null
        && metadata != null
        && metadata.statusCode() != null
        && metadata.redirectCount() != null
        && metadata.cookieCount() != null
        && capture().mediaType() != null
        && capture().mediaEncoding() != null;
  }

  public boolean hasHydratedSource() {
    return hydratedSource != null
        && hydratedSource.length > 0;
  }

  public boolean hasSourceSnapshot() {
    return capture() != null
        && capture().sourceReference() != null;
  }

  public boolean hasHydratedText() {
    return hydratedText != null
        && !hydratedText.isBlank();
  }

  public boolean hasTextSnapshot() {
    return capture() != null
        && capture().interpretedReference() != null;
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
    return mediaMetadata() == null
        ? null
        : mediaMetadata().statusCode();
  }

  public Integer redirectHopCount() {
    return mediaMetadata() == null
        ? null
        : mediaMetadata().redirectCount();
  }

  public Integer setCookieCount() {
    return mediaMetadata() == null
        ? null
        : mediaMetadata().cookieCount();
  }

  public MediaType mediaType() {
    return capture() == null
        ? null
        : capture().mediaType();
  }

  public MediaEncoding characterEncoding() {
    return mediaEncoding();
  }

  public MediaEncoding mediaEncoding() {
    return capture() == null
        ? null
        : capture().mediaEncoding();
  }

  public Long contentLength() {
    return mediaMetadata() == null
        ? null
        : mediaMetadata().contentLength();
  }

  public String previewText() {
    return contentDocument == null
        ? null
        : contentDocument.previewText();
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

  private MediaMetadata mediaMetadata() {
    return capture() == null
        ? null
        : capture().mediaMetadata();
  }

  private Charset resolveCharset() {
    MediaEncoding encoding = mediaEncoding();
    if (encoding == null || encoding.getCanonicalName() == null) {
      return StandardCharsets.UTF_8;
    }
    try {
      return Charset.forName(encoding.getCanonicalName());
    } catch (Exception exception) {
      return StandardCharsets.UTF_8;
    }
  }

  public AcquiredContent withContentDocument(ContentDocument nextDocument) {
    return new AcquiredContent(requestHeader, target, reviewId, nextDocument, hydratedSource, hydratedText,
        completeText);
  }

  public AcquiredContent withHydratedSource(byte[] nextSource) {
    return new AcquiredContent(requestHeader, target, reviewId, contentDocument, nextSource, hydratedText,
        completeText);
  }

  public AcquiredContent withHydratedText(String nextText, boolean nextCompleteText) {
    return new AcquiredContent(requestHeader, target, reviewId, contentDocument, hydratedSource, nextText,
        nextCompleteText);
  }
}
