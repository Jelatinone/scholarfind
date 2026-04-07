package com.github.jelatinone.models.investigate;

import java.util.Map;
import java.util.Set;

public record ClassificationStub(
    Map<ClassificationKind, Double> contributions,
    double confidence,
    Set<ClassificationKind> contenders) {
}
