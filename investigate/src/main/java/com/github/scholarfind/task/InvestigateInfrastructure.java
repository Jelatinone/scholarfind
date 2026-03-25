package com.github.scholarfind.task;

import java.util.UUID;

import com.github.scholarfind.api.store.Store;
import com.github.scholarfind.meta.PipelineTask.Infrastructure;
import com.github.scholarfind.models.annotate.AnnotateRequest;
import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.models.investigate.InvestigateRequest;
import com.github.scholarfind.models.shared.ContextDocument;

/**
 *
 * <h1>InvestigateInfrastructure</h1>
 *
 * <p>
 * Describes the stage-specific infrastructure required by
 * {@link InvestigateTask InvestigateTask}.
 *
 * @author Cody Washington
 */
public interface InvestigateInfrastructure extends Infrastructure<InvestigateRequest, AnnotateRequest> {

  Store<InvestigateDocument, UUID> investigateStore();

  Store<ContextDocument, UUID> contextStore();
}
