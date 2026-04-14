package com.github.jelatinone.task.policy;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.ingest.IngestOrigin;
import com.github.jelatinone.models.ingest.IngestProvenance;
import com.github.jelatinone.models.ingest.IngestRequest;
import com.github.jelatinone.models.investigate.ClassificationKind;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.TargetReference;
import com.github.jelatinone.policy.EmissionIntent;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyStep;
import com.github.jelatinone.task.InvestigateContext;
import com.github.jelatinone.task.InvestigateState;
import com.github.jelatinone.task.signal.extractors.AnchorDensityExtractor;

public final class InvestigateReflowPolicy implements Policy<InvestigateContext, InvestigateState> {

	@Override
	public PolicyStep<InvestigateState> apply(InvestigateContext context, InvestigateState state) {
		ClassificationSelection selection = ClassificationSelection.resolve(
				state.classification(),
				context.classificationConfiguration());
		if (!selection.minimumConfidenceExceeded() || selection.dominantKind() != ClassificationKind.AGGREGATOR) {
			return new PolicyStep.Continue<>(state);
		}

		AcquiredContent acquired = availableContent(context);
		if (acquired == null || !AnchorDensityExtractor.isHTMLCapable(acquired)) {
			return new PolicyStep.Continue<>(state);
		}

		List<EmissionIntent<? extends Request>> nextEmissions = new ArrayList<>(state.emissions());
		List<EmissionIntent<? extends Request>> discoveredEmissions = discoverEmissions(context, acquired);
		nextEmissions.addAll(discoveredEmissions);

		InvestigateState nextState = state.withEmissions(nextEmissions)
				.withDiscoveredTargetCount(discoveredEmissions.size());
		return new PolicyStep.Continue<>(nextState);
	}

	private AcquiredContent availableContent(InvestigateContext context) {
		ContentDocument retrievedContent = context.retrievedContent();
		if (retrievedContent == null
				|| retrievedContent.capture() == null
				|| retrievedContent.capture().sourceSnapshot() == null) {
			return null;
		}
		return context.acquisitionService().ensureContent(new AcquiredContent(
				context.document().requestHeader(),
				context.document().target(),
				context.document().trace(),
				retrievedContent,
				null,
				null,
				false));
	}

	private List<EmissionIntent<? extends Request>> discoverEmissions(
			InvestigateContext context,
			AcquiredContent acquisition) {
		return acquisition.decodedSource()
				.map(source -> {
					Document document = Jsoup.parse(source, resolveBaseUrl(acquisition));
					Map<UUID, TargetReference> discoveredTargets = new LinkedHashMap<>();
					for (Element anchor : document.select("a[href]")) {
						String href = anchor.absUrl("href");
						if (href.isBlank()) {
							continue;
						}
						TargetReference target = resolveTarget(context, href);
						if (target == null || discoveredTargets.containsKey(target.targetId())) {
							continue;
						}
						if (context.document().target() != null
								&& context.document().target().targetId().equals(target.targetId())) {
							continue;
						}
						discoveredTargets.put(target.targetId(), target);
					}
					List<EmissionIntent<? extends Request>> emissions = new ArrayList<>();
					discoveredTargets.values().forEach(target -> emissions.add(new EmissionIntent<>(
							new IngestRequest(
									context.document().requestHeader(),
									target,
									new IngestProvenance(
											IngestOrigin.REFLOW,
											null,
											context.document().documentHeader().documentId(),
											context.document().target() == null
													? null
													: context.document().target().targetId()),
									null),
							null,
							ProcessingStage.INGEST,
							buildKey(context.document().requestHeader(), target))));
					return List.copyOf(emissions);
				})
				.orElse(List.of());
	}

	private TargetReference resolveTarget(InvestigateContext context, String href) {
		try {
			UUID parentTargetId = context.document().target() == null
					? null
					: context.document().target().targetId();
			int nextDepth = context.document().target() == null
					? 0
					: context.document().target().depth() + 1;
			Instant discoveredAt = context.reviewedAt();
			return TargetReference.canonical(href, parentTargetId, nextDepth, discoveredAt);
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

	private String buildKey(RequestHeader header, TargetReference target) {
		if (header == null || header.idempotencyKey() == null || target == null || target.targetId() == null) {
			return null;
		}
		return String.format("%s:%s", header.idempotencyKey(), target.targetId());
	}
}
