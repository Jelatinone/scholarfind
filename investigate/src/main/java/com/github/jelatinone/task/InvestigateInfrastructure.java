package com.github.jelatinone.task;

import java.util.UUID;

import com.github.jelatinone.acquisition.AcquisitionService;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.meta.PipelineTask.Infrastructure;
import com.github.jelatinone.models.annotate.AnnotateRequest;
import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.investigate.InvestigateDocument;
import com.github.jelatinone.models.investigate.InvestigateRequest;

public interface InvestigateInfrastructure extends Infrastructure<InvestigateRequest, AnnotateRequest> {

  Store<InvestigateDocument, UUID> investigateStore();

  Store<ContentDocument, UUID> contentStore();

  AcquisitionService acquisitionService();
}
