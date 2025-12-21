package com.github.scholarfind.models.content;

import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Trace;

public record ContextDocument(
        Header header,
        Trace trace,
        String rawContent,
        String fingerprint) {

    @Override
    public String toString() {
        return header().id().toString();
    }
}
