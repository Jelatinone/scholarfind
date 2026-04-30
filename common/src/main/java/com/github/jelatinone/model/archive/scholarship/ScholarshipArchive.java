package com.github.jelatinone.model.archive.scholarship;

import java.util.Collection;
import java.util.List;

import com.github.jelatinone.model.archive.Archive;
import com.github.jelatinone.model.archive.ArchiveHeader;
import com.github.jelatinone.model.archive.scholarship.dossier.Description;
import com.github.jelatinone.model.archive.scholarship.dossier.Requirement;

public record ScholarshipArchive(

		ArchiveHeader archiveHeader,

		List<Description<?>> descriptionItems,

		Collection<Requirement<?>> applicationRequirements,
		Collection<Requirement<?>> eligibilityRequirements

) implements Archive<ScholarshipArchive> {

	@Override
	public ScholarshipArchive withArchiveHeader(ArchiveHeader header) {
		return new ScholarshipArchive(
				header,
				descriptionItems(),
				applicationRequirements(),
				eligibilityRequirements());
	}

}
