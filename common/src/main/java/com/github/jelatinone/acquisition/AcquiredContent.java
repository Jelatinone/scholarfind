package com.github.jelatinone.acquisition;

import java.net.URL;

import com.github.jelatinone.models.content.Capture;
import com.github.jelatinone.models.content.ContentDocument;
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

  public URL effectiveUrl() {
    return capture() == null
        ? null
        : capture().effectiveUrl();
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

  public AcquiredContent withContentDocument(ContentDocument nextDocument) {
    return new AcquiredContent(requestHeader, target, trace, nextDocument, hydratedSource, hydratedText, completeText);
  }

  public AcquiredContent withHydratedSource(byte[] nextSource) {
    return new AcquiredContent(requestHeader, target, trace, contentDocument, nextSource, hydratedText, completeText);
  }

  public AcquiredContent withHydratedText(String nextText, boolean nextCompleteText) {
    return new AcquiredContent(requestHeader, target, trace, contentDocument, hydratedSource, nextText, nextCompleteText);
  }
}
