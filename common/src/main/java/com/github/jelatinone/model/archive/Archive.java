package com.github.jelatinone.model.archive;

import lombok.NonNull;

public interface Archive<Self> {

	@NonNull
	ArchiveHeader archiveHeader();

	Self withArchiveHeader(@NonNull ArchiveHeader header);

}
