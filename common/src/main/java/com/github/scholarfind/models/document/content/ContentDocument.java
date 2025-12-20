package com.github.scholarfind.models.document.content;

import com.github.scholarfind.models.document.Header;
import com.github.scholarfind.models.document.Trace;

public record ContentDocument(
                Header header,
                Trace trace,
                String rawContent,
                String fingerprint) {
}
