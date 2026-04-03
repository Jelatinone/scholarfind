package com.github.jelatinone.infra;

import java.time.Duration;
import java.util.UUID;

import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.infra.queue.AnnotateRequestQueue;
import com.github.jelatinone.infra.queue.InvestigateRequestQueue;
import com.github.jelatinone.infra.repository.AttemptEventStore;
import com.github.jelatinone.infra.repository.ContentDocumentStore;
import com.github.jelatinone.infra.repository.InvestigateDocumentStore;
import com.github.jelatinone.infra.repository.StageExecutionRecordStore;
import com.github.jelatinone.models.annotate.AnnotateRequest;
import com.github.jelatinone.models.audit.AttemptEvent;
import com.github.jelatinone.models.audit.StageExecution;
import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.investigate.InvestigateDocument;
import com.github.jelatinone.models.investigate.InvestigateRequest;
import com.github.jelatinone.models.shared.StageEnvelope;
import com.github.jelatinone.task.InvestigateInfrastructure;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class AWSInvestigateInfrastructure implements InvestigateInfrastructure {
  MetricPublisher metrics;
  SqsClient sqsClient;
  DynamoDbClient dynamoClient;

  RetryableQueue<StageEnvelope<InvestigateRequest>> inQueue;
  Queue<StageEnvelope<AnnotateRequest>> outQueue;

  Store<AttemptEvent, String> eventStore;
  Store<StageExecution, String> executionStore;
  Store<InvestigateDocument, UUID> investigateStore;
  Store<ContentDocument, UUID> contentStore;

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {
    @Builder.Default
    String inQueueName = "queue_investigate",
        outQueueName = "queue_annotate",
        retryQueueName = "queue_investigate_retry",
        errorQueueName = "queue_investigate_error";

    @Builder.Default
    String investigateStoreName = "store_investigate",
        contentStoreName = "store_content",
        attemptEventStoreName = "store_attempt_event",
        executionStoreName = "store_stage_execution";

    @Builder.Default
    Duration callTimeout = Duration.ofSeconds(30);
  }

  @Override
  public RetryableQueue<StageEnvelope<InvestigateRequest>> inQueue() {
    return inQueue;
  }

  @Override
  public Queue<StageEnvelope<AnnotateRequest>> outQueue() {
    return outQueue;
  }

  @Override
  public Store<AttemptEvent, String> eventStore() {
    return eventStore;
  }

  @Override
  public Store<StageExecution, String> executionStore() {
    return executionStore;
  }

  @Override
  public Store<InvestigateDocument, UUID> investigateStore() {
    return investigateStore;
  }

  @Override
  public Store<ContentDocument, UUID> contentStore() {
    return contentStore;
  }

  public static AWSInvestigateInfrastructure create(final @NonNull Configuration config) {
    MetricPublisher metrics = CloudWatchMetricPublisher.builder()
        .cloudWatchClient(CloudWatchAsyncClient.create())
        .detailedMetrics(CoreMetric.API_CALL_DURATION, CoreMetric.API_CALL_SUCCESSFUL)
        .build();

    SqsClient sqsClient = SqsClient.builder()
        .overrideConfiguration(overrides -> overrides
            .addMetricPublisher(metrics)
            .apiCallAttemptTimeout(config.callTimeout))
        .build();

    DynamoDbClient dynamoClient = DynamoDbClient.builder()
        .overrideConfiguration(overrides -> overrides
            .addMetricPublisher(metrics)
            .apiCallAttemptTimeout(config.callTimeout))
        .build();

    return new AWSInvestigateInfrastructure(
        metrics,
        sqsClient,
        dynamoClient,
        new InvestigateRequestQueue(
            sqsClient,
            resolveQueueUrl(sqsClient, config.inQueueName),
            resolveQueueUrl(sqsClient, config.retryQueueName),
            resolveQueueUrl(sqsClient, config.errorQueueName)),
        new AnnotateRequestQueue(
            sqsClient,
            resolveQueueUrl(sqsClient, config.outQueueName)),
        new AttemptEventStore(dynamoClient, config.attemptEventStoreName),
        new StageExecutionRecordStore(dynamoClient, config.executionStoreName),
        new InvestigateDocumentStore(dynamoClient, config.investigateStoreName),
        new ContentDocumentStore(dynamoClient, config.contentStoreName));
  }

  private static String resolveQueueUrl(final @NonNull SqsClient sqsClient, final @NonNull String canonicalName) {
    return sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(canonicalName)
            .build())
        .queueUrl();
  }

  @Override
  public void close() throws Exception {
    metrics.close();
    sqsClient.close();
    dynamoClient.close();
  }
}