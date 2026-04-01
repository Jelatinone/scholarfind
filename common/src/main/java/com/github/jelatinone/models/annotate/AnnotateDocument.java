package com.github.jelatinone.models.annotate;

import java.time.LocalDate;
import java.util.Collection;

import com.github.jelatinone.models.dossier.Activity;
import com.github.jelatinone.models.dossier.EducationLevel;
import com.github.jelatinone.models.dossier.Location;
import com.github.jelatinone.models.dossier.PursuedDegreeLevel;
import com.github.jelatinone.models.dossier.SupplementalType;
import com.github.jelatinone.models.shared.DocumentHeader;
import com.github.jelatinone.models.shared.FetchReference;
import com.github.jelatinone.models.shared.ReasonCode;
import com.github.jelatinone.models.shared.RequestHeader;
import com.github.jelatinone.models.shared.StageDocument;
import com.github.jelatinone.models.shared.TargetReference;
import com.github.jelatinone.models.shared.TraceReference;

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

  public static final long SCHEMA_VERSION = 1L;

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
