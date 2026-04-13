package com.github.jelatinone.models.scholarship;

import java.util.Date;
import java.net.URL;
import java.util.Collection;

import com.github.jelatinone.models.dossier.Activity;
import com.github.jelatinone.models.dossier.EducationLevel;
import com.github.jelatinone.models.dossier.Location;
import com.github.jelatinone.models.dossier.PursuedDegreeLevel;
import com.github.jelatinone.models.dossier.SupplementalType;
import com.github.jelatinone.models.shared.DocumentHeader;

public record ScholarshipDocument(
		DocumentHeader header,
		String organizationName,
		String scholarshipName,
		Date openDate,
		Date closeDate,
		Double awardAmount,
		Collection<Location> location,
		Collection<Activity> activities,
		Collection<SupplementalType> supplements,
		Collection<PursuedDegreeLevel> pursuedDegrees,
		Collection<EducationLevel> educationLevels,
		URL reference) {
	public static final long SCHEMA_VERSION = 1L;
}
