package com.github.jelatinone.model.investigate;

import java.time.Instant;

import com.github.jelatinone.feature.FeatureMapset;
import com.github.jelatinone.model.classification.Category;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;

public record InvestigateFeedback(
    TargetIdentity targetId,
    ReviewIdentity reviewId,

    FeatureMapset features,

    Category category,
    Double confidence,

    Instant emittedAt) {

}
