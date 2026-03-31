package com.github.scholarfind.task;

import java.util.UUID;

import com.github.scholarfind.api.store.Store;
import com.github.scholarfind.meta.PipelineTask.Infrastructure;
import com.github.scholarfind.models.ingest.IngestDocument;
import com.github.scholarfind.models.ingest.IngestRequest;
import com.github.scholarfind.models.ingest.TargetRecord;
import com.github.scholarfind.models.investigate.InvestigateRequest;

public interface IngestInfrastructure extends Infrastructure<IngestRequest, InvestigateRequest> {

  Store<IngestDocument, UUID> ingestStore();

  Store<TargetRecord, String> targetStore();

  IngestPersistResult persist(IngestDocument document, TargetRecord currentRecord, TargetRecord nextRecord);
}
