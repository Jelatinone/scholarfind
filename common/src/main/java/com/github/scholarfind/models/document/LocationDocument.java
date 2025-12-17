package com.github.scholarfind.models.document;

public record LocationDocument(
    LocationLevel level,

    String country,
    String state,
    String county,
    String city,
    String town,
    String zip) {
}
