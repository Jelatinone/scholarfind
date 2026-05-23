package com.github.jelatinone.feature;

import java.util.Set;

import com.github.jelatinone.acquisition.Acquisition;

public interface FeatureExtractor<T> {

  Set<FeatureCategory> categories();

  Set<Feature<T>> extract(Acquisition content);

  boolean supports(Acquisition content);
}
