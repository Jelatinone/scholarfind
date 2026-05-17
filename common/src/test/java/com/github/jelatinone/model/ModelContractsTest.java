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
import com.github.jelatinone.model.graph.GraphReviewCause;
import com.github.jelatinone.model.graph.GraphEdge;
import com.github.jelatinone.model.graph.GraphNode;
import com.github.jelatinone.model.graph.GraphReview;
import com.github.jelatinone.model.graph.GraphReviewState;
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
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyOutcome;

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

    GraphNode.Target node = new GraphNode.Target(targetId, CanonicalTestFixtures.url("https://example.com"), now);
    GraphReview review = new GraphReview(reviewId, targetId,
        new GraphReviewCause.Origin("seed"),
        new GraphReviewState.Created(ExecutionStage.DISCOVERY, "Task-1", now),
        now);
    GraphEdge.Parent edge = new GraphEdge.Parent(edgeId, reviewId, UUID.randomUUID(), targetId, now);
    GraphNode.Entity entity = new GraphNode.Entity(entityId, reviewId, now);
    GraphEdge.Reduce membership = new GraphEdge.Reduce(UUID.randomUUID(), reviewId, targetId, entityId, now);

    assertEquals(GraphNode.Target.SCHEMA_VERSION, node.schemaVersion());
    assertEquals(GraphReview.SCHEMA_VERSION, review.schemaVersion());
    assertEquals(GraphEdge.Parent.SCHEMA_VERSION, edge.schemaVersion());
    assertEquals(GraphNode.Entity.SCHEMA_VERSION, entity.schemaVersion());
    assertEquals(GraphEdge.Reduce.SCHEMA_VERSION, membership.schemaVersion());
    assertEquals(targetId, node.canonicalId());
    assertEquals(entityId, entity.canonicalId());
    assertEquals(edge.edgeId(), edge.canonicalId());
    assertEquals(membership.edgeId(), membership.canonicalId());
    assertEquals(targetId, node.properties().get("targetId"));
    assertEquals(entityId, membership.properties().get("entityId"));
    assertInstanceOf(GraphReviewCause.Origin.class, review.causedBy());
  }

  @Test
  void structTransitAndInvestigateModels_updateHeaders() {
    RequestHeader requestHeader = StructTestFixtures.requestHeader(1, ExecutionStage.INVESTIGATE);
    DocumentHeader documentHeader = StructTestFixtures.documentHeader(ExecutionStage.INVESTIGATE);
    InvestigateRequest request = new InvestigateRequest(requestHeader, StructTestFixtures.TARGET_ID,
        StructTestFixtures.REVIEW_ID);
    InvestigateDocument document = StructTestFixtures.document(1, ExecutionStage.INVESTIGATE);
    Emission<InvestigateRequest> emission = new Emission<>(request, Duration.ofSeconds(5), ExecutionStage.ANNOTATE);

    assertEquals(2, RequestHeader.retry(requestHeader, StructTestFixtures.NOW).attempt());
    assertEquals(0, RequestHeader.next(requestHeader, StructTestFixtures.NOW, 99L).attempt());
    assertEquals(documentHeader, document.documentHeader());
    assertEquals(requestHeader, request.requestHeader());
    assertEquals(documentHeader, document.withDocumentHeader(documentHeader).documentHeader());
    assertEquals(requestHeader, document.withRequestHeader(requestHeader).requestHeader());
    assertEquals(Duration.ofSeconds(5), emission.delay());
  }

  @Test
  void auditRecords_bindDomainDirective_andReasons() {
    AttemptTransition transition = new AttemptTransition(PolicyOutcome.RETRY, PolicyReason.OPERATION_EXCEPTION,
        StructTestFixtures.NOW);
    ExecutionEvent executionEvent = new ExecutionEvent(
        StructTestFixtures.TARGET_ID,
        ExecutionStage.INVESTIGATE,
        Set.of(transition),
        PolicyOutcome.NEXT,
        StructTestFixtures.NOW,
        StructTestFixtures.NOW);
    AttemptEvent attemptEvent = new AttemptEvent(
        UUID.randomUUID(),
        StructTestFixtures.REVIEW_ID,
        StructTestFixtures.TARGET_ID,
        Set.of(PolicyReason.REQUEST_REJECTED),
        PolicyOutcome.ERROR,
        StructTestFixtures.NOW,
        StructTestFixtures.NOW);

    assertEquals("REQUEST_REJECTED", PolicyReason.REQUEST_REJECTED.reason());
    assertEquals(PolicyOutcome.ERROR, attemptEvent.disposition());
    assertEquals(PolicyOutcome.NEXT, executionEvent.outcome());
  }

  @Test
  void archiveAndScholarshipModels_preserveHeaders() {
    ArchiveHeader header = new ArchiveHeader(
        UUID.randomUUID(),
        StructTestFixtures.REVIEW_ID,
        new ArchiveState.Active(ExecutionStage.PUBLISH, "Task-1", StructTestFixtures.NOW),
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
  void archiveStates_requireReviewMetadata() {
    assertThrows(NullPointerException.class,
        () -> new ArchiveState.Pending(null, "Task-1", StructTestFixtures.NOW));
    assertThrows(NullPointerException.class,
        () -> new ArchiveState.Active(ExecutionStage.PUBLISH, null, StructTestFixtures.NOW));
    assertThrows(NullPointerException.class,
        () -> new ArchiveState.Expired(ExecutionStage.PUBLISH, "Task-1", null));
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
