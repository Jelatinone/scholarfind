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
import com.github.jelatinone.model.transit.Letter;

class KernelArchetypeContractsTest {

  @Test
  void reviewedOperable_requiresReviewToReferenceTarget() {
    GraphNode.Target target = target(StructTestFixtures.TARGET_ID);
    GraphReview review = review(StructTestFixtures.TARGET_ID);

    KernelOperable.Reviewed operable = new KernelOperable.Reviewed(target, review);

    assertEquals(target, operable.target());
    assertEquals(review, operable.review());
    assertThrows(
        IllegalArgumentException.class,
        () -> new KernelOperable.Reviewed(target, review(UUID.randomUUID())));
  }

  @Test
  void operables_requireTargetsAndReviews() {
    GraphNode.Target target = target(StructTestFixtures.TARGET_ID);
    GraphReview review = review(StructTestFixtures.TARGET_ID);

    assertThrows(NullPointerException.class, () -> new KernelOperable.Unreviewed(null));
    assertThrows(NullPointerException.class, () -> new KernelOperable.Reviewed(null, review));
    assertThrows(NullPointerException.class, () -> new KernelOperable.Reviewed(target, null));
  }

  @Test
  void postable_copiesLettersIntoImmutableCollection() {
    KernelOperable.Unreviewed operable = new KernelOperable.Unreviewed(target(StructTestFixtures.TARGET_ID));
    Letter<InvestigateRequest> letter = StructTestFixtures.letter(0, ExecutionStage.INVESTIGATE);
    List<Letter<InvestigateRequest>> source = new ArrayList<>(List.of(letter));

    KernelPostable<InvestigateRequest> postable = new KernelPostable<>(operable, source);
    source.clear();

    assertEquals(List.of(letter), List.copyOf(postable.letters()));
    assertThrows(UnsupportedOperationException.class, () -> postable.letters().clear());
  }

  private static GraphNode.Target target(UUID targetId) {
    return new GraphNode.Target(
        targetId,
        CanonicalTestFixtures.url("https://example.com"),
        StructTestFixtures.NOW);
  }

  private static GraphReview review(UUID targetId) {
    return new GraphReview(
        StructTestFixtures.REVIEW_ID,
        targetId,
        new GraphReviewCause.Origin("seed"),
        new GraphReviewState.Created(ExecutionStage.DISCOVERY, "seed", StructTestFixtures.NOW),
        StructTestFixtures.NOW);
  }
}
