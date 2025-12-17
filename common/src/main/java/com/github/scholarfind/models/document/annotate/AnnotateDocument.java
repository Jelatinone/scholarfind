package com.github.scholarfind.models.document.annotate;

import java.time.chrono.ChronoLocalDate;
import java.util.Collection;

import com.github.scholarfind.models.document.HeaderDocument;
import com.github.scholarfind.models.document.LocationDocument;

public record AnnotateDocument(

    HeaderDocument header,

    String organizationName,
    String scholarshipName,
    LocationDocument location,

    ChronoLocalDate openDate,
    ChronoLocalDate closeDate,

    Double awardAmount,

    Collection<ActivityDocument> activities,
    Collection<SupplementalType> supplements,

    Collection<PursuedDegreeLevel> pursuedDegrees,
    Collection<EducationLevel> educationLevels) {

}
