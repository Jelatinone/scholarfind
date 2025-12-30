package com.github.scholarfind.models;

import java.net.URL;

public record Trace(
        URL url,
        URL parentUrl,

        String reviewer,

        Integer depth,
        Integer attempt) {

}
