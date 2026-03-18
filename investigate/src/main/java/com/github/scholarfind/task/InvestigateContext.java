package com.github.scholarfind.task;

import java.time.Instant;

import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.models.shared.ContextDocument;
import com.github.scholarfind.policy.PolicyContext;
import com.github.scholarfind.task.policy.ClassificationConfiguration;

public record InvestigateContext(
    InvestigateDocument document,

    ClassificationConfiguration classificationConfiguration,

    Instant reviewedAt,

    InvestigateDocument retrievedInvestigate,
    ContextDocument retrievedContext

) implements PolicyContext<InvestigateDocument> {

}
