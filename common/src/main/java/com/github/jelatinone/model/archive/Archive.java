package com.github.jelatinone.model.archive;

public interface Archive<Self> {

	ArchiveHeader archiveHeader();

	Self withArchiveHeader(ArchiveHeader header);

}
