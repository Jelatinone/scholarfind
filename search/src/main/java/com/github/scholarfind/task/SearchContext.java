package com.github.scholarfind.task;

import java.time.Instant;

import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.models.shared.ContextDocument;
import com.github.scholarfind.task.validation.ClassificationConfiguration;
import com.github.scholarfind.validation.ValidationContext;

public record SearchContext(
    InvestigateDocument document,

    ClassificationConfiguration classificationConfiguration,

    Instant reviewedAt,

    InvestigateDocument retrievedInvestigate,
    ContextDocument retrievedContext

) implements ValidationContext<InvestigateDocument> {

}
