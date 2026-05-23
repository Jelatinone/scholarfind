package com.github.jelatinone.meta.archetype.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.jelatinone.fixtures.CanonicalTestFixtures;
import com.github.jelatinone.fixtures.StructTestFixtures;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.graph.GraphNode;
import com.github.jelatinone.model.graph.GraphReview;
import com.github.jelatinone.model.graph.GraphReviewCause;
import com.github.jelatinone.model.graph.GraphReviewState;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;
import com.github.jelatinone.model.struct.Request;

class KernelArchetypeContractsTest {

  @Test
  void reviewedOperable_requiresReviewToReferenceTarget() {
    GraphNode.Target target = target(StructTestFixtures.TARGET_ID);
    GraphReview review = review(StructTestFixtures.TARGET_ID);

    KernelOperand.Reviewed<GraphNode.Target> operable = new KernelOperand.Reviewed<GraphNode.Target>(target, review);

    assertEquals(target, operable.target());
    assertEquals(review, operable.review());
    assertThrows(
        IllegalArgumentException.class,
        () -> new KernelOperand.Reviewed<GraphNode.Target>(target, review(TargetIdentity.create(UUID.randomUUID()))));
  }

  @Test
  void operables_requireTargetsAndReviews() {
    GraphNode.Target target = target(StructTestFixtures.TARGET_ID);
    GraphReview review = review(StructTestFixtures.TARGET_ID);

    assertThrows(NullPointerException.class, () -> new KernelOperand.Unreviewed<GraphNode.Target>(null));
    assertThrows(NullPointerException.class, () -> new KernelOperand.Reviewed<GraphNode.Target>(null, review));
    assertThrows(NullPointerException.class, () -> new KernelOperand.Reviewed<GraphNode.Target>(target, null));
  }

  @Test
  void postable_copiesRequestsIntoImmutableCollection() {
    KernelOperand.Unreviewed<GraphNode.Target> operable = new KernelOperand.Unreviewed<GraphNode.Target>(
        target(StructTestFixtures.TARGET_ID));
    InvestigateRequest request = StructTestFixtures.request(0, ExecutionStage.INVESTIGATE);
    List<Request<InvestigateRequest>> source = new ArrayList<>(List.of(request));

    KernelResult<InvestigateRequest, GraphNode.Target> postable = new KernelResult<>(operable, source);
    source.clear();

    assertEquals(List.of(request), List.copyOf(postable.letters()));
    assertThrows(UnsupportedOperationException.class, () -> postable.letters().clear());
  }

  private static GraphNode.Target target(TargetIdentity targetId) {
    GraphNode.Target target = GraphNode.Target.create(CanonicalTestFixtures.url("https://example.com"),
        StructTestFixtures.NOW);
    return GraphNode.Target.create(target.domainId(), targetId, target.canonicalUrl(), target.emittedAt());
  }

  private static GraphReview review(TargetIdentity targetId) {
    return GraphReview.create(
        StructTestFixtures.REVIEW_ID,
        targetId,
        new GraphReviewCause.Origin("seed"),
        new GraphReviewState.Available(ExecutionStage.DISCOVERY, "seed", StructTestFixtures.NOW),
        StructTestFixtures.NOW);
  }
}
