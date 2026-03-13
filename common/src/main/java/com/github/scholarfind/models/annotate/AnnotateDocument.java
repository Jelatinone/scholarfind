package com.github.scholarfind.models.annotate;

import java.time.LocalDate;
import java.util.Collection;

import com.github.scholarfind.models.dossier.Activity;
import com.github.scholarfind.models.dossier.EducationLevel;
import com.github.scholarfind.models.dossier.Location;
import com.github.scholarfind.models.dossier.PursuedDegreeLevel;
import com.github.scholarfind.models.dossier.SupplementalType;
import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.FetchReference;
import com.github.scholarfind.models.shared.ReasonCode;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageDocument;
import com.github.scholarfind.models.shared.TargetReference;
import com.github.scholarfind.models.shared.TraceReference;

public record AnnotateDocument(
    DocumentHeader documentHeader,
    RequestHeader requestHeader,
    TargetReference target,
    TraceReference trace,
    FetchReference snapshot,
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
    Double extractionConfidence,
    Collection<ReasonCode> qualityFlags,
    int discoveredTargetCount) implements StageDocument<AnnotateDocument> {

  public static final long schemaVersion = 1L;

  @Override
  public AnnotateDocument withDocumentHeader(DocumentHeader header) {
    return new AnnotateDocument(
        header,
        requestHeader,
        target,
        trace,
        snapshot,
        organizationName,
        scholarshipName,
        openDate,
        closeDate,
        awardAmount,
        location,
        activities,
        supplements,
        pursuedDegrees,
        educationLevels,
        extractionConfidence,
        qualityFlags,
        discoveredTargetCount);
  }

  @Override
  public AnnotateDocument withRequestHeader(RequestHeader header) {
    return new AnnotateDocument(
        documentHeader,
        header,
        target,
        trace,
        snapshot,
        organizationName,
        scholarshipName,
        openDate,
        closeDate,
        awardAmount,
        location,
        activities,
        supplements,
        pursuedDegrees,
        educationLevels,
        extractionConfidence,
        qualityFlags,
        discoveredTargetCount);
  }
}
