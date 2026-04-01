package com.github.jelatinone.policy;

import com.github.jelatinone.models.shared.StageDocument;

public interface PolicyContext<D extends StageDocument<D>> {

  D document();
}
