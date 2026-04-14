package com.github.jelatinone.task.signal;

import java.util.*;

import com.github.jelatinone.acquisition.AcquiredContent;
import com.github.jelatinone.acquisition.AcquisitionService;
import com.github.jelatinone.models.investigate.ClassificationKind;
import com.github.jelatinone.models.investigate.ClassificationStub;
import com.github.jelatinone.task.evidence.EvidenceRule;
import com.github.jelatinone.task.policy.ClassificationConfiguration;
import com.github.jelatinone.task.score.ScoreRule;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class SignalPlanner {
	static double EPSILON = 1E-6D;

	ClassificationConfiguration _configuration;
	AcquisitionService _service;
	List<SignalExtractor> _extractors;

	private record Candidate(
			SignalExtractor extractor,
			double cost,
			double potential,
			double utility,
			int order) {
	}

	public SignalPlanner(
			@NonNull ClassificationConfiguration configuration,
			@NonNull AcquisitionService acquisitionService) {
		_configuration = configuration;
		_service = acquisitionService;
		_extractors = List.copyOf(configuration.signalExtractors());
	}

	public ClassificationStub classify(@NonNull AcquiredContent acquisition) {
		AcquiredContent currentAcquisition = ensure(acquisition, _configuration.plannerConfiguration().initialTier());
		Map<ClassificationKind, Double> contributions = new HashMap<>();
		Set<SignalIdentity> executed = new HashSet<>();

		while (true) {
			ClassificationStub stub = buildStub(contributions);
			if (stub.confidence() >= _configuration.dominanceConfiguration().minimumConfidence()) {
				return stub;
			}

			Candidate candidate = locateCandidate(currentAcquisition, stub, executed, true);
			if (candidate != null) {
				apply(candidate.extractor(), currentAcquisition, contributions);
				executed.add(candidate.extractor().identity());
				continue;
			}

			candidate = locateCandidate(currentAcquisition, stub, executed, false);
			if (candidate == null
					|| candidate.utility() < _configuration.plannerConfiguration().minimumUtility()) {
				stub = buildStub(contributions);
				return stub;
			}

			if (!_configuration.plannerConfiguration().maxTier().includes(candidate.extractor().requires())) {
				stub = buildStub(contributions);
				return stub;
			}

			currentAcquisition = ensure(currentAcquisition, candidate.extractor().requires());
			apply(candidate.extractor(), currentAcquisition, contributions);
			executed.add(candidate.extractor().identity());
		}
	}

	private AcquiredContent ensure(AcquiredContent acquisition, SignalTier tier) {
		return switch (tier) {
			case TRACE -> acquisition;
			case METADATA -> _service.ensureMetadata(acquisition);
			case CONTENT -> _service.ensureContent(acquisition);
			case TEXT -> _service.ensureText(acquisition);
		};
	}

	private Candidate locateCandidate(
			AcquiredContent acquisition,
			ClassificationStub stub,
			Set<SignalIdentity> executed,
			boolean mustBeCostless) {
		return _extractors.stream()
				.filter(extractor -> !executed.contains(extractor.identity()))
				.filter(extractor -> extractor.supports(acquisition))
				.map(extractor -> buildCandidate(extractor, acquisition, stub))
				.filter(Objects::nonNull)
				.filter(candidate -> !mustBeCostless || candidate.cost() <= EPSILON)
				.filter(
						candidate -> mustBeCostless
								|| candidate.utility() >= _configuration.plannerConfiguration().minimumUtility())
				.min(Comparator
						.comparingDouble(Candidate::utility)
						.reversed()
						.thenComparing(candidate -> candidate.extractor().requires().ordinal())
						.thenComparing((candidate) -> -candidate.potential())
						.thenComparingInt(Candidate::order))
				.orElse(null);
	}

	private Candidate buildCandidate(
			SignalExtractor extractor,
			AcquiredContent acquisition,
			ClassificationStub stub) {
		double potential = potential(extractor.identity(), stub);
		if (potential <= 0D) {
			return null;
		}
		double cost = Math.max(0D, extractor.cost(acquisition, stub, _configuration.plannerConfiguration().costPattern()));
		double utility = potential / Math.max(EPSILON, cost);
		return new Candidate(extractor, cost, potential, utility, _extractors.indexOf(extractor));
	}

	private void apply(
			SignalExtractor extractor,
			AcquiredContent acquisition,
			Map<ClassificationKind, Double> contributions) {
		Optional<SignalValue> value = extractor.extract(acquisition);
		List<EvidenceRule> rules = _configuration.evidenceConfiguration().rulesFor(extractor.identity());
		rules.forEach(rule -> {
			ScoreRule scoreRule = _configuration.scoreConfiguration().policyFor(rule.classification());
			if (scoreRule == null) {
				return;
			}
			double raw = rule.rule().apply(value);
			double weighted = scoreRule.score(raw);
			double sanitized = Double.isFinite(weighted) ? weighted : 0D;
			contributions.merge(rule.classification(), sanitized, Double::sum);
		});
	}

	private double potential(SignalIdentity signal, ClassificationStub stub) {
		Set<ClassificationKind> contenders = stub.contenders().isEmpty()
				? EnumSet.copyOf(Arrays.asList(ClassificationKind.values()))
				: stub.contenders();

		return _configuration.evidenceConfiguration().rulesFor(signal).stream()
				.filter(rule -> contenders.contains(rule.classification()))
				.map(rule -> _configuration.scoreConfiguration().policyFor(rule.classification()))
				.filter(Objects::nonNull)
				.mapToDouble((rule) -> Math.max(
						Math.abs(rule.min()),
						Math.abs(rule.max())))
				.sum();
	}

	private ClassificationStub buildStub(Map<ClassificationKind, Double> contributions) {
		if (contributions.isEmpty()) {
			return new ClassificationStub(
					Map.of(),
					0D,
					EnumSet.copyOf(Arrays.asList(ClassificationKind.values())));
		}
		double leader = contributions.values().stream()
				.mapToDouble(Double::doubleValue)
				.max()
				.orElse(0D);
		EnumSet<ClassificationKind> contenders = contributions.entrySet().stream()
				.filter(entry -> leader - entry.getValue() <= _configuration.dominanceConfiguration().dominanceEpsilon())
				.map(Map.Entry::getKey)
				.collect(
						() -> EnumSet.noneOf(ClassificationKind.class),
						EnumSet::add,
						EnumSet::addAll);
		return new ClassificationStub(Map.copyOf(contributions), leader, contenders);
	}
}
