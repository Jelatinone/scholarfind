package com.github.scholarfind.task;

import java.time.ZonedDateTime;

import com.github.scholarfind.models.context.ContextDocument;
import com.github.scholarfind.models.search.SearchDocument;
import com.github.scholarfind.task.validation.ClassificationConfiguration;
import com.github.scholarfind.validation.ValidationContext;

public record SearchContext(
    SearchDocument document,

    ClassificationConfiguration classificationConfiguration,

    ZonedDateTime reviewedAt,

    SearchDocument retreivedSearch,
    ContextDocument retrievedContext

) implements ValidationContext<SearchDocument> {

}
