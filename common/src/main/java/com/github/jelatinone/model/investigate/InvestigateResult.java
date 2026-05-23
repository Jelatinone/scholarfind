package com.github.jelatinone.model.investigate;

import java.util.Set;

import com.github.jelatinone.feature.FeatureMapset;
import com.github.jelatinone.model.classification.Classification;
import com.github.jelatinone.model.struct.Identity.EdgeIdentity;

public interface InvestigateResult {

  FeatureMapset features();

  Set<EdgeIdentity> producedEdges();

  record Classified(
      Classification.Investigate classification,
      FeatureMapset features,

      Set<EdgeIdentity> producedEdges) implements InvestigateResult {
  }

  record Forwarded(
      FeatureMapset features,

      Set<EdgeIdentity> producedEdges) implements InvestigateResult {
  }
}
