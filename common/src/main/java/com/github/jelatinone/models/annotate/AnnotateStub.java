package com.github.jelatinone.models.annotate;

import java.util.Collection;
import java.util.Date;

import com.github.jelatinone.models.dossier.Activity;
import com.github.jelatinone.models.dossier.EducationLevel;
import com.github.jelatinone.models.dossier.Location;
import com.github.jelatinone.models.dossier.PursuedDegreeLevel;
import com.github.jelatinone.models.dossier.SupplementalType;

public record AnnotateStub(
    String organizationName,
    String scholarshipName,
    Date openDate,
    Date closeDate,
    Double awardAmount,
    Collection<Location> location,
    Collection<Activity> activities,
    Collection<SupplementalType> supplements,
    Collection<PursuedDegreeLevel> pursuedDegrees,
    Collection<EducationLevel> educationLevels) {

}
