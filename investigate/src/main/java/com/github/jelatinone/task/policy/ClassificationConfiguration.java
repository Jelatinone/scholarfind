package com.github.jelatinone.task.policy;

import java.time.Duration;

import com.github.jelatinone.task.evidence.EvidenceIdentifierConfiguration;
import com.github.jelatinone.task.score.DominanceConfiguration;
import com.github.jelatinone.task.score.ScoreRuleConfiguration;
import com.github.jelatinone.task.signal.SignalCostConfiguration;
import com.github.jelatinone.task.signal.SignalExtractorConfiguration;

public record ClassificationConfiguration(
        SignalCostConfiguration costConfiguration,
        SignalExtractorConfiguration extractorConfiguration,
        EvidenceIdentifierConfiguration evidenceConfiguration,
        ScoreRuleConfiguration scoreConfiguration,
        DominanceConfiguration dominanceConfiguration,
        Duration reuseWindowDays) {
}
