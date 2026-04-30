package com.github.jelatinone.model.archive;

import java.net.URL;
import java.util.Collection;

import com.github.jelatinone.model.dossier.Activity;
import com.github.jelatinone.model.dossier.EducationLevel;
import com.github.jelatinone.model.dossier.Location;
import com.github.jelatinone.model.dossier.DegreeLevel;
import com.github.jelatinone.model.dossier.Supplemental;

public record ScholarshipArchive(
		ArchiveHeader archiveHeader,

		URL canonicalUrl,
		URL applicationUrl,

		String organizationName,
		String scholarshipName,

		String description,
		String summary,

		ScholarshipWindow window,
		ScholarshipAward award,

		Collection<Location> locations,
		Collection<Activity> activities,
		Collection<Supplemental> supplements,
		Collection<DegreeLevel> degreeLevels,
		Collection<EducationLevel> educationLevels,

		Collection<String> fieldsOfStudy,
		Collection<String> affiliations,
		Collection<String> eligibilityDescriptors,
		Collection<String> requirementDescriptors

) implements Archive<ScholarshipArchive> {

	@Override
	public ScholarshipArchive withArchiveHeader(ArchiveHeader header) {
		return new ScholarshipArchive(
				header,
				canonicalUrl(),
				applicationUrl(),
				organizationName(),
				scholarshipName(),
				summary(),
				description(),
				window(),
				award(),
				locations(),
				activities(),
				supplements(),
				degreeLevels(),
				educationLevels(),
				fieldsOfStudy(),
				affiliations(),
				eligibilityDescriptors(),
				requirementDescriptors());
	}

}
