package com.github.jelatinone.task;

import java.util.UUID;

import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.meta.PipelineTask.Infrastructure;
import com.github.jelatinone.models.ingest.IngestDocument;
import com.github.jelatinone.models.ingest.IngestRequest;
import com.github.jelatinone.models.ingest.TargetRecord;
import com.github.jelatinone.models.investigate.InvestigateRequest;

public interface IngestInfrastructure extends Infrastructure<IngestRequest, InvestigateRequest> {

  Store<IngestDocument, UUID> ingestStore();

  Store<TargetRecord, String> targetStore();

  IngestPersistResult persist(IngestDocument document, TargetRecord currentRecord, TargetRecord nextRecord);
}
