package com.github.jelatinone.task.signal;

import com.github.jelatinone.models.investigate.ClassificationStub;

public record SignalPlannerResult(
    ClassificationStub classification,
    double confidence) {
}
