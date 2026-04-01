package com.github.jelatinone.models.investigate;

import java.util.Map;

public record Classification(
        Map<ClassificationType, Double> contributions) {
}
