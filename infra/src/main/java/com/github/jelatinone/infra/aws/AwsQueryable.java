package com.github.jelatinone.infra.aws;

import java.util.Optional;

public interface AWSQueryable<Create, Created> {

  Optional<Created> tryCreate(Create create);

  default Created mustCreate(Create create) {
    return tryCreate(create)
        .orElseThrow(() -> new IllegalStateException("AWS resource could not be created"));
  }
}
