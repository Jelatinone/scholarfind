package com.github.scholarfind.models.search;

import java.util.Map;

public record Classification(
        Map<ClassificationType, Double> classifications) {
}
