package com.github.scholarfind.utility;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Trace;
import com.github.scholarfind.models.search.Classification;

public interface Builder<Builds extends Document<?>> {
  Builds build();

  Builder<Builds> setTrace(Trace trace);

  Builder<Builds> setClassification(Classification classification);

  Builder<Builds> setHeader(Header header);

}
