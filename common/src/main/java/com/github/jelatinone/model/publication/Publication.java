package com.github.jelatinone.model.publication;

import lombok.NonNull;

public interface Publication<Self> {

  @NonNull
  PublicationHeader archiveHeader();

  Self withArchiveHeader(@NonNull PublicationHeader header);

}
