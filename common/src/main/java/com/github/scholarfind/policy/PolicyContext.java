package com.github.scholarfind.policy;

import com.github.scholarfind.models.shared.StageDocument;

public interface PolicyContext<D extends StageDocument<D>> {

  D document();
}
