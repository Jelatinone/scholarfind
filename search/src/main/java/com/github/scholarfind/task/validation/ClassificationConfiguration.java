package com.github.scholarfind.task.validation;

import com.github.scholarfind.task.evidence.EvidenceIdentifierConfiguration;
import com.github.scholarfind.task.score.ScoreRuleConfiguration;
import com.github.scholarfind.task.signal.SignalCostConfiguration;
import com.github.scholarfind.task.signal.SignalExtractorConfiguration;

public record ClassificationConfiguration(
    SignalCostConfiguration costConfiguration, SignalExtractorConfiguration extractorConfiguration,
    EvidenceIdentifierConfiguration evidenceConfiguration, ScoreRuleConfiguration scoreConfiguration) {
}
