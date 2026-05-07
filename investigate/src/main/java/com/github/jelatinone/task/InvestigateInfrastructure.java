package com.github.jelatinone.task;

import java.util.UUID;

import com.github.jelatinone.acquisition.AcquisitionService;
import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.meta.construct.Infrastructure;
import com.github.jelatinone.model.annotate.AnnotateRequest;
import com.github.jelatinone.model.content.ContentDocument;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.transit.Letter;

public interface InvestigateInfrastructure extends Infrastructure<InvestigateRequest, AnnotateRequest> {

  RetryableQueue<Letter<InvestigateRequest>> input();

  Store<InvestigateDocument, UUID> investigateStore();

  Store<ContentDocument, UUID> contentStore();

  AcquisitionService acquisitionService();
}
