package com.github.jelatinone.feature;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class FeatureMapset {

  EnumMap<FeatureCategory, Feature<?>> features;

  public FeatureMapset() {
    this.features = new EnumMap<>(FeatureCategory.class);
  }

  private FeatureMapset(EnumMap<FeatureCategory, Feature<?>> features) {
    this.features = new EnumMap<>(FeatureCategory.class) {
      {
        values()
            .forEach(feature -> features.put(feature.category(), feature));
      }
    };
  }

  public Feature<?> put(Feature<?> feature) {
    return features.put(feature.category(), feature);
  }

  public boolean has(FeatureCategory category) {
    return features.containsKey(category);
  }

  public Optional<Feature<?>> get(FeatureCategory category) {
    return Optional.ofNullable(features.get(category));
  }

  public FeatureMapset with(Collection<? extends Feature<?>> nextFeatures) {
    EnumMap<FeatureCategory, Feature<?>> copy = new EnumMap<>(features) {
      {
        nextFeatures.forEach((feat) -> put(feat.category(), feat));
      }
    };
    return new FeatureMapset(copy);
  }
}