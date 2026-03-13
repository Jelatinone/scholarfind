package com.github.scholarfind.models.scholarship;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

import com.github.scholarfind.models.dossier.Activity;
import com.github.scholarfind.models.dossier.EducationLevel;
import com.github.scholarfind.models.dossier.PursuedDegreeLevel;
import com.github.scholarfind.models.dossier.SupplementalType;
import com.github.scholarfind.models.dossier.Location;

public record ScholarshipDocument(
    UUID scholarshipId,
    long version,
    String organizationName,
    String scholarshipName,
    LocalDate openDate,
    LocalDate closeDate,
    Double awardAmount,
    Collection<Location> location,
    Collection<Activity> activities,
    Collection<SupplementalType> supplements,
    Collection<PursuedDegreeLevel> pursuedDegrees,
    Collection<EducationLevel> educationLevels,
    String applicationUrl) {
}
