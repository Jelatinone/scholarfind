package com.github.scholarfind.utility;

import com.github.scholarfind.models.Document;
import com.github.scholarfind.models.Header;

public interface Builder<Builds extends Document<?>> {
  Builds build();

  Builder<Builds> setHeader(Header header);

}
