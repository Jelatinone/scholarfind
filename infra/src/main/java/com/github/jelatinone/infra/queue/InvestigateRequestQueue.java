package com.github.jelatinone.infra.queue;

import com.github.jelatinone.models.investigate.InvestigateRequest;

import software.amazon.awssdk.services.sqs.SqsClient;

public final class InvestigateRequestQueue extends StageEnvelopeQueue<InvestigateRequest> {
    public InvestigateRequestQueue(
            SqsClient client,
            String inputUrl,
            String retryUrl,
            String errorUrl) {
        super(client, inputUrl, retryUrl, errorUrl, InvestigateRequest.class);
    }
}
