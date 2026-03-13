package com.github.scholarfind.models.investigate;

import java.util.Map;

public record Classification(
    Map<ClassificationType, Double> contributions) {
}
