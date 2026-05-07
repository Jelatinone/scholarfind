package com.github.jelatinone.task.policy;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.model.content.ContentDocument;
import com.github.jelatinone.model.investigate.Category;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.InvestigateContext;
import com.github.jelatinone.task.InvestigateState;
import com.github.jelatinone.task.signal.extractors.AnchorDensityExtractor;
import com.github.jelatinone.utility.Canonical;

public final class InvestigateReflowPolicy implements Policy<InvestigateContext, InvestigateState> {

  @Override
  public PolicyStep<InvestigateState> apply(InvestigateContext context, InvestigateState state) {
    ClassificationSelection selection = ClassificationSelection.resolve(
        state.classification(),
        context.classificationConfiguration());
    if (!selection.minimumConfidenceExceeded() || selection.dominantKind() != Category.AGGREGATOR) {
      return new PolicyStep.Continue<>(state);
    }

    AcquiredContent acquired = availableContent(context);
    if (acquired == null || !AnchorDensityExtractor.isHTMLCapable(acquired)) {
      return new PolicyStep.Continue<>(state);
    }

    return new PolicyStep.Continue<>(state.withDiscoveredTargetCount(discoveredTargetCount(context, acquired)));
  }

  private AcquiredContent availableContent(InvestigateContext context) {
    ContentDocument retrievedContent = context.retrievedContent();
    if (retrievedContent == null
        || retrievedContent.capture() == null
        || retrievedContent.capture().sourceCapture() == null) {
      return null;
    }
    return context.acquisitionService().ensureContent(new AcquiredContent(
        context.document().requestHeader(),
        context.target(),
        context.envelopeReviewId(),
        retrievedContent,
        null,
        null,
        false));
  }

  private int discoveredTargetCount(
      InvestigateContext context,
      AcquiredContent acquisition) {
    return acquisition.decodedSource()
        .map(source -> {
          Document document = Jsoup.parse(source, resolveBaseUrl(acquisition));
          Set<UUID> discoveredTargets = new LinkedHashSet<>();
          for (Element anchor : document.select("a[href]")) {
            String href = anchor.absUrl("href");
            if (href.isBlank()) {
              continue;
            }
            UUID targetId = resolveTargetId(href);
            if (targetId == null) {
              continue;
            }
            if (context.target() != null && context.target().targetId().equals(targetId)) {
              continue;
            }
            discoveredTargets.add(targetId);
          }
          return discoveredTargets.size();
        })
        .orElse(0);
  }

  private UUID resolveTargetId(String href) {
    try {
      return Canonical.generateTargetUUID(Canonical.canonicalizeURL(href));
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  private String resolveBaseUrl(AcquiredContent acquisition) {
    URL url = acquisition.effectiveUrl() != null
        ? acquisition.effectiveUrl()
        : acquisition.targetUrl();
    return url == null
        ? ""
        : url.toExternalForm();
  }
}
