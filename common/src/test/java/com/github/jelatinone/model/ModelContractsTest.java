package com.github.jelatinone.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.AcquisitionTestFixtures;
import com.github.jelatinone.fixtures.CanonicalTestFixtures;
import com.github.jelatinone.fixtures.StructTestFixtures;
import com.github.jelatinone.meta.transitory.Directive;
import com.github.jelatinone.model.archive.ArchiveHeader;
import com.github.jelatinone.model.archive.ArchiveState;
import com.github.jelatinone.model.audit.AttemptEvent;
import com.github.jelatinone.model.audit.AttemptTransition;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.content.Capture;
import com.github.jelatinone.model.content.MediaEncoding;
import com.github.jelatinone.model.content.MediaMetadata;
import com.github.jelatinone.model.content.MediaType;
import com.github.jelatinone.model.graph.EntityEdge;
import com.github.jelatinone.model.graph.EntityNode;
import com.github.jelatinone.model.graph.TargetCause;
import com.github.jelatinone.model.graph.TargetEdge;
import com.github.jelatinone.model.graph.TargetNode;
import com.github.jelatinone.model.graph.TargetReview;
import com.github.jelatinone.model.graph.TargetState;
import com.github.jelatinone.model.investigate.Category;
import com.github.jelatinone.model.investigate.Classification;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.scholarship.ScholarshipArchive;
import com.github.jelatinone.model.scholarship.dossier.Description;
import com.github.jelatinone.model.scholarship.dossier.DescriptionKind;
import com.github.jelatinone.model.scholarship.dossier.Magnitude;
import com.github.jelatinone.model.scholarship.dossier.Requirement;
import com.github.jelatinone.model.scholarship.dossier.RequirementKind;
import com.github.jelatinone.model.scholarship.dossier.description.Award;
import com.github.jelatinone.model.scholarship.dossier.description.Window;
import com.github.jelatinone.model.scholarship.dossier.requirement.Activity;
import com.github.jelatinone.model.scholarship.dossier.requirement.Degree;
import com.github.jelatinone.model.scholarship.dossier.requirement.Education;
import com.github.jelatinone.model.scholarship.dossier.requirement.Location;
import com.github.jelatinone.model.struct.DocumentHeader;
import com.github.jelatinone.model.struct.RequestHeader;
import com.github.jelatinone.model.transit.Emission;
import com.github.jelatinone.model.transit.Letter;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.StageOutcome;

class ModelContractsTest {

  @Test
  void mediaTypesAndEncodings_resolveExpectedHeaders() {
    assertEquals(MediaType.TEXT_HTML, MediaType.resolve("text/html; charset=UTF-8"));
    assertEquals(MediaType.APPLICATION_JSON, MediaType.resolve("application/json"));
    assertEquals(MediaType.OTHER, MediaType.resolve(null));

    assertEquals(MediaEncoding.UTF_8, MediaEncoding.resolve("text/plain; charset=utf-8"));
    assertEquals(MediaEncoding.WINDOWS_1252, MediaEncoding.resolve("text/plain; charset=windows-1252"));
    assertEquals(MediaEncoding.UTF_8, MediaEncoding.resolve(null));
  }

  @Test
  void graphRecords_defaultSchemaVersionsAndPayloads() {
    Instant now = StructTestFixtures.NOW;
    UUID edgeId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    UUID targetId = StructTestFixtures.TARGET_ID;
    UUID reviewId = StructTestFixtures.REVIEW_ID;

    TargetNode node = new TargetNode(targetId, CanonicalTestFixtures.url("https://example.com"), now);
    TargetReview review = new TargetReview(reviewId, targetId,
        new TargetCause.Origin("seed"), new TargetState.Created(ExecutionStage.DISCOVERY, "Task-1"), now);
    TargetEdge edge = new TargetEdge(edgeId, reviewId, UUID.randomUUID(), targetId, now);
    EntityNode entity = new EntityNode(entityId, reviewId, now);
    EntityEdge membership = new EntityEdge(UUID.randomUUID(), targetId, entityId, reviewId, now);

    assertEquals(TargetNode.SCHEMA_VERSION, node.schemaVersion());
    assertEquals(TargetReview.SCHEMA_VERSION, review.schemaVersion());
    assertEquals(TargetEdge.SCHEMA_VERSION, edge.schemaVersion());
    assertEquals(EntityNode.SCHEMA_VERSION, entity.schemaVersion());
    assertEquals(EntityEdge.SCHEMA_VERSION, membership.schemaVersion());
    assertInstanceOf(TargetCause.Origin.class, review.causedBy());
  }

  @Test
  void structTransitAndInvestigateModels_updateHeaders() {
    RequestHeader requestHeader = StructTestFixtures.requestHeader(1, ExecutionStage.INVESTIGATE);
    DocumentHeader documentHeader = StructTestFixtures.documentHeader(ExecutionStage.INVESTIGATE);
    InvestigateRequest request = new InvestigateRequest(requestHeader, StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID);
    InvestigateDocument document = StructTestFixtures.document(1, ExecutionStage.INVESTIGATE);
    Letter<InvestigateRequest> letter = new Letter<>(
        request.targetId(),
        request.reviewId(),
        ExecutionStage.INVESTIGATE,
        request,
        StructTestFixtures.NOW);
    Emission<InvestigateRequest> emission = new Emission<>(request, Duration.ofSeconds(5), ExecutionStage.ANNOTATE);

    assertEquals(2, RequestHeader.retry(requestHeader, StructTestFixtures.NOW).attempt());
    assertEquals(0, RequestHeader.next(requestHeader, StructTestFixtures.NOW, 99L).attempt());
    assertEquals(documentHeader, document.documentHeader());
    assertEquals(requestHeader, request.requestHeader());
    assertEquals(documentHeader, document.withDocumentHeader(documentHeader).documentHeader());
    assertEquals(requestHeader, document.withRequestHeader(requestHeader).requestHeader());
    assertEquals(Letter.SCHEMA_VERSION, letter.schemaVersion());
    assertEquals(Duration.ofSeconds(5), emission.delay());
  }

  @Test
  void auditRecords_bindDomainDirective_andReasons() {
    AttemptTransition transition = new AttemptTransition(StageOutcome.RETRY, PolicyReason.OPERATION_EXCEPTION,
        StructTestFixtures.NOW);
    ExecutionEvent executionEvent = new ExecutionEvent(
        StructTestFixtures.TARGET_ID,
        ExecutionStage.INVESTIGATE,
        Set.of(transition),
        StageOutcome.NEXT,
        StructTestFixtures.NOW,
        StructTestFixtures.NOW);
    AttemptEvent attemptEvent = new AttemptEvent(
        UUID.randomUUID(),
        StructTestFixtures.REVIEW_ID,
        StructTestFixtures.TARGET_ID,
        Set.of(PolicyReason.REQUEST_REJECTED),
        Directive.ERROR,
        StructTestFixtures.NOW,
        StructTestFixtures.NOW);

    assertEquals("REQUEST_REJECTED", PolicyReason.REQUEST_REJECTED.reason());
    assertEquals(Directive.ERROR, attemptEvent.disposition());
    assertEquals(StageOutcome.NEXT, executionEvent.outcome());
  }

  @Test
  void archiveAndScholarshipModels_preserveHeaders() {
    ArchiveHeader header = new ArchiveHeader(
        UUID.randomUUID(),
        StructTestFixtures.REVIEW_ID,
        ArchiveState.ACTIVE,
        StructTestFixtures.NOW);
    ScholarshipArchive archive = new ScholarshipArchive(
        header,
        Set.of(new Description<>(DescriptionKind.SCHOLARSHIP_NAME, "Hope Scholarship")),
        "summary",
        "description",
        Set.of(new Requirement<>(RequirementKind.STUDENT_ACTIVITY, Activity.LEADERSHIP, Magnitude.MUST_HAVE, null)),
        Set.of(new Requirement<>(RequirementKind.STUDENT_LOCATION, Location.STATE, Magnitude.MAY_HAVE, null)));

    assertEquals(header, archive.archiveHeader());
    assertEquals(header, archive.withArchiveHeader(header).archiveHeader());
  }

  @Test
  void dossierKinds_validateBoundValues() {
    Description<String> description = new Description<>(DescriptionKind.ORGANIZATION_NAME, "OpenAI");
    Requirement<Activity> requirement = new Requirement<>(
        RequirementKind.STUDENT_ACTIVITY,
        Activity.STEM,
        Magnitude.MUST_HAVE,
        "club");
    Award award = new Award(BigDecimal.ONE, BigDecimal.TEN, "USD", true, 2, "Annual");
    Window window = new Window(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-01"), "Spring");
    Classification.Interpreted classification = new Classification.Interpreted(
        Map.of(Category.ARCHIVE, 0.8d),
        0.8d,
        Set.of(Category.ARCHIVE),
        Category.ARCHIVE,
        true);
    Capture capture = new Capture.Resolved(StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID, CanonicalTestFixtures.url("https://example.com"), "body".getBytes(), "hash",
        MediaType.TEXT_PLAIN,
        MediaEncoding.UTF_8, new MediaMetadata(200, 0, 0, null, null), StructTestFixtures.NOW);
    MediaMetadata mediaMetadata = AcquisitionTestFixtures.mediaMetadata("text/plain", 4);

    assertEquals("OpenAI", description.description());
    assertEquals(Activity.STEM, requirement.requirement());
    assertEquals("USD", award.currencyCode());
    assertEquals(LocalDate.parse("2026-02-01"), window.closeDate());
    assertTrue(DescriptionKind.AWARD.accepts(award));
    assertTrue(RequirementKind.STUDENT_ACTIVITY.accepts(Activity.WORK));
    assertFalse(RequirementKind.STUDENT_ACTIVITY.accepts(Degree.MBA));
    assertFalse(DescriptionKind.APPLICATION_WINDOW.accepts(Education.GRADUATE));
    assertEquals(Category.ARCHIVE, classification.mostPragmaticCategory());
    assertEquals(4L, mediaMetadata.contentLength());
    assertEquals(StructTestFixtures.TARGET_ID, capture.targetId());

    assertThrows(IllegalArgumentException.class,
        () -> new Description<>(DescriptionKind.CANONICAL_URL, "not-a-url"));
    assertThrows(IllegalArgumentException.class,
        () -> new Requirement<>(RequirementKind.STUDENT_PURSUED_DEGREE, Activity.ARTS, Magnitude.MAY_HAVE, null));
  }
}
