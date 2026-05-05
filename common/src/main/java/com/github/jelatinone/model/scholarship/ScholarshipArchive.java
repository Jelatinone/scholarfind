package com.github.jelatinone.model.scholarship;

import java.util.Set;

import com.github.jelatinone.model.archive.Archive;
import com.github.jelatinone.model.archive.ArchiveHeader;
import com.github.jelatinone.model.scholarship.dossier.Description;
import com.github.jelatinone.model.scholarship.dossier.Requirement;

import lombok.NonNull;

public record ScholarshipArchive(

    @NonNull ArchiveHeader archiveHeader,

    Set<Description<?>> itemDescriptions,

    String summary,
    String description,

    Set<Requirement<?>> applicationRequirements,
    Set<Requirement<?>> eligibilityRequirements

) implements Archive<ScholarshipArchive> {

  @Override
  public ScholarshipArchive withArchiveHeader(ArchiveHeader header) {
    return new ScholarshipArchive(
        header,
        itemDescriptions(),
        summary(),
        description(),
        applicationRequirements(),
        eligibilityRequirements());
  }

}
