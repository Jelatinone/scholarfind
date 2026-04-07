package com.github.jelatinone.task.signal;

public record SignalPlannerConfiguration(
    SignalTier initialTier,
    SignalTier maxTier,
    SignalPattern costPattern,
    double minimumUtility) {

}
