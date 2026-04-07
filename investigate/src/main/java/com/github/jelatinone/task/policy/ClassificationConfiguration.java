package com.github.jelatinone.task.policy;

import java.time.Duration;
import java.util.Collection;

import com.github.jelatinone.task.evidence.EvidenceIdentifierConfiguration;
import com.github.jelatinone.task.score.DominanceConfiguration;
import com.github.jelatinone.task.score.ScoreRuleConfiguration;
import com.github.jelatinone.task.signal.SignalExtractor;
import com.github.jelatinone.task.signal.SignalPlannerConfiguration;

public record ClassificationConfiguration(
    Collection<SignalExtractor> signalExtractors,
    SignalPlannerConfiguration plannerConfiguration,
    EvidenceIdentifierConfiguration evidenceConfiguration,
    ScoreRuleConfiguration scoreConfiguration,
    DominanceConfiguration dominanceConfiguration,
    Duration reuseWindowDays) {
}
