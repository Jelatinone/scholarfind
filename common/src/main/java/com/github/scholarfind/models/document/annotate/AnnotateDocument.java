package com.github.scholarfind.models.document.annotate;

import java.time.chrono.ChronoLocalDate;
import java.util.Collection;

import com.github.scholarfind.models.document.HeaderDocument;
import com.github.scholarfind.models.document.LocationDocument;
import com.github.scholarfind.models.document.TraceDocument;

public record AnnotateDocument(
                HeaderDocument header,
                TraceDocument trace,

                String organizationName,
                String scholarshipName,

                ChronoLocalDate openDate,
                ChronoLocalDate closeDate,

                Double awardAmount,

                Collection<LocationDocument> location,

                Collection<ActivityDocument> activities,
                Collection<SupplementalType> supplements,

                Collection<PursuedDegreeLevel> pursuedDegrees,
                Collection<EducationLevel> educationLevels) {

}
