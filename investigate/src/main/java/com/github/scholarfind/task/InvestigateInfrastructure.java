package com.github.scholarfind.task;

import java.util.UUID;

import com.github.scholarfind.api.store.Store;
import com.github.scholarfind.meta.PipelineTask.Infrastructure;
import com.github.scholarfind.models.annotate.AnnotateRequest;
import com.github.scholarfind.models.content.ContentDocument;
import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.models.investigate.InvestigateRequest;

public interface InvestigateInfrastructure extends Infrastructure<InvestigateRequest, AnnotateRequest> {

  Store<InvestigateDocument, UUID> investigateStore();

  Store<ContentDocument, UUID> contentStore();
}
