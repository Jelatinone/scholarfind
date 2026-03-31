package com.github.scholarfind.infra;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.github.scholarfind.api.queue.Queue;
import com.github.scholarfind.api.queue.RetryableQueue;
import com.github.scholarfind.api.store.Store;
import com.github.scholarfind.infra.aws.DynamoStore;
import com.github.scholarfind.infra.aws.SqsQueue;
import com.github.scholarfind.infra.queue.StageEnvelopeQueue;
import com.github.scholarfind.infra.repository.AttemptEventStore;
import com.github.scholarfind.infra.repository.IngestDocumentStore;
import com.github.scholarfind.infra.repository.StageExecutionRecordStore;
import com.github.scholarfind.infra.repository.TargetRecordStore;
import com.github.scholarfind.models.audit.AttemptEvent;
import com.github.scholarfind.models.audit.StageExecution;
import com.github.scholarfind.models.ingest.IngestDocument;
import com.github.scholarfind.models.ingest.IngestRequest;
import com.github.scholarfind.models.ingest.TargetRecord;
import com.github.scholarfind.models.investigate.InvestigateRequest;
import com.github.scholarfind.models.shared.StageEnvelope;
import com.github.scholarfind.task.IngestInfrastructure;
import com.github.scholarfind.task.IngestPersistResult;

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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.sqs.SqsClient;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class AWSIngestInfrastructure implements IngestInfrastructure {
  MetricPublisher metrics;
  SqsClient sqsClient;
  DynamoDbClient dynamoClient;

  SqsQueue<StageEnvelope<IngestRequest>> inQueue;
  SqsQueue<StageEnvelope<InvestigateRequest>> outQueue;

  DynamoStore<AttemptEvent, String> eventStore;
  DynamoStore<StageExecution, String> executionStore;
  DynamoStore<IngestDocument, UUID> ingestStore;
  DynamoStore<TargetRecord, String> targetStore;

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {
    @Builder.Default
    String inQueueName = "queue_ingest",
        outQueueName = "queue_investigate",
        retryQueueName = "queue_ingest_retry",
        errorQueueName = "queue_ingest_error";

    @Builder.Default
    String ingestStoreName = "store_ingest",
        targetStoreName = "store_target",
        attemptEventStoreName = "store_attempt_event",
        executionStoreName = "store_stage_execution";

    @Builder.Default
    Duration callTimeout = Duration.ofSeconds(30);
  }

  @Override
  public RetryableQueue<StageEnvelope<IngestRequest>> inQueue() {
    return inQueue;
  }

  @Override
  public Queue<StageEnvelope<InvestigateRequest>> outQueue() {
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
  public Store<IngestDocument, UUID> ingestStore() {
    return ingestStore;
  }

  @Override
  public Store<TargetRecord, String> targetStore() {
    return targetStore;
  }

  public static AWSIngestInfrastructure create(final @NonNull Configuration config) {
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

    return new AWSIngestInfrastructure(
        metrics,
        sqsClient,
        dynamoClient,
        new StageEnvelopeQueue<>(
            sqsClient,
            resolveQueueUrl(sqsClient, config.inQueueName),
            resolveQueueUrl(sqsClient, config.retryQueueName),
            resolveQueueUrl(sqsClient, config.errorQueueName),
            IngestRequest.class),
        new StageEnvelopeQueue<>(
            sqsClient,
            resolveQueueUrl(sqsClient, config.outQueueName),
            null,
            null,
            InvestigateRequest.class),
        new AttemptEventStore(dynamoClient, config.attemptEventStoreName),
        new StageExecutionRecordStore(dynamoClient, config.executionStoreName),
        new IngestDocumentStore(dynamoClient, config.ingestStoreName),
        new TargetRecordStore(dynamoClient, config.targetStoreName));
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

  @Override
  public IngestPersistResult persist(@NonNull IngestDocument document, TargetRecord currentRecord,
      TargetRecord nextRecord) {
    if (nextRecord == null) {
      ingestStore.put(document);
      return IngestPersistResult.APPLIED;
    }

    if (!document.admitted()) {
      dynamoClient.transactWriteItems(builder -> builder
          .transactItems(
              ingestStore.transactPut(document),
              targetStore.transactPut(nextRecord))
          .build());
      return IngestPersistResult.APPLIED;
    }

    try {
      dynamoClient.transactWriteItems(builder -> builder
          .transactItems(
              ingestStore.transactPut(document),
              putConditionally(currentRecord, nextRecord))
          .build());
      return IngestPersistResult.APPLIED;
    } catch (TransactionCanceledException exception) {
      boolean admissionConflictOccured = exception.cancellationReasons() != null
          && exception.cancellationReasons().stream()
              .anyMatch(reason -> "ConditionalCheckFailed".equals(reason.code()));
      if (admissionConflictOccured) {
        return IngestPersistResult.ADMISSION_CONFLICT;
      }
      throw exception;
    }
  }

  private TransactWriteItem putConditionally(
      TargetRecord currentRecord,
      TargetRecord nextRecord) {
    if (currentRecord == null) {
      return targetStore.transactPut(
          nextRecord,
          builder -> builder.conditionExpression("attribute_not_exists(id)"));
    }

    AttributeValue expectedPayload = targetStore.encode(currentRecord).get("payload");
    return targetStore.transactPut(
        nextRecord,
        builder -> builder
            .conditionExpression("payload = :expectedPayload")
            .expressionAttributeValues(Map.of(":expectedPayload", expectedPayload)));
  }
}
