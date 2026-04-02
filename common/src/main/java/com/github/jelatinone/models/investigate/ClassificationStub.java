package com.github.jelatinone.models.investigate;

import java.util.Map;

public record ClassificationStub(
    Map<ClassificationKind, Double> contributions) {
}
