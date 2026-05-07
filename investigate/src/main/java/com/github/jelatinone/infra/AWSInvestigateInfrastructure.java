package com.github.jelatinone.infra;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.github.jelatinone.acquisition.AcquisitionService;
import com.github.jelatinone.acquisition.BodyFetcher;
import com.github.jelatinone.acquisition.ContentInterpreter;
import com.github.jelatinone.acquisition.MetadataFetcher;
import com.github.jelatinone.acquisition.PersistentAcquisitionService;
import com.github.jelatinone.acquisition.fetch.HTTPBodyFetcher;
import com.github.jelatinone.acquisition.fetch.HTTPMetadataFetcher;
import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.infra.aws.queue.SQSQueue.SQSQueueDescriptor;
import com.github.jelatinone.infra.aws.store.DynamoStore;
import com.github.jelatinone.infra.aws.store.S3Store;
import com.github.jelatinone.infra.aws.store.serial.ByteArrayS3Serializer;
import com.github.jelatinone.infra.aws.store.serial.JacksonDynamoSerializer;
import com.github.jelatinone.infra.construct.ExecutionRouter;
import com.github.jelatinone.infra.queue.LetterQueue;
import com.github.jelatinone.infra.store.AttemptEventStore;
import com.github.jelatinone.infra.store.ExecutionEventStore;
import com.github.jelatinone.meta.construct.Router;
import com.github.jelatinone.model.annotate.AnnotateRequest;
import com.github.jelatinone.model.audit.AttemptEvent;
import com.github.jelatinone.model.audit.ExecutionEvent;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.content.CaptureReference;
import com.github.jelatinone.model.content.ContentDocument;
import com.github.jelatinone.model.investigate.InvestigateDocument;
import com.github.jelatinone.model.investigate.InvestigateRequest;
import com.github.jelatinone.model.transit.Letter;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class AWSInvestigateInfrastructure implements InvestigateInfrastructure {
  MetricPublisher metrics;
  SqsClient sqsClient;
  DynamoDbClient dynamoClient;
  S3Client s3Client;

  Router router;
  RetryableQueue<Letter<InvestigateRequest>> input;

  @SuppressWarnings("unused")
  Queue<Letter<AnnotateRequest>> annotateQueue;

  Store<AttemptEvent, String> attemptStore;
  Store<ExecutionEvent, String> executionStore;
  Store<InvestigateDocument, UUID> investigateStore;
  Store<ContentDocument, UUID> contentStore;
  AcquisitionService acquisitionService;

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {
    @Builder.Default
    String inQueueName = "queue_investigate",
        annotateQueueName = "queue_annotate",
        retryQueueName = "queue_investigate_retry",
        errorQueueName = "queue_investigate_error";

    @Builder.Default
    String investigateStoreName = "store_investigate",
        contentStoreName = "store_content",
        attemptEventStoreName = "store_attempt_event",
        executionStoreName = "store_stage_execution";

    @Builder.Default
    String captureBucketName = "store_capture",
        sourceCapturePrefix = "capture/source",
        textCapturePrefix = "capture/text";

    @Builder.Default
    List<ContentInterpreter> interpreters = List.of();

    @Builder.Default
    Duration callTimeout = Duration.ofSeconds(30);

    @Builder.Default
    int maxRedirects = 5;

    @Builder.Default
    String userAgent = UUID.randomUUID().toString();
  }

  @Override
  public Router router() {
    return router;
  }

  @Override
  public Store<AttemptEvent, String> attemptStore() {
    return attemptStore;
  }

  @Override
  public Store<ExecutionEvent, String> executionStore() {
    return executionStore;
  }

  @Override
  public RetryableQueue<Letter<InvestigateRequest>> input() {
    return input;
  }

  @Override
  public Store<InvestigateDocument, UUID> investigateStore() {
    return investigateStore;
  }

  @Override
  public Store<ContentDocument, UUID> contentStore() {
    return contentStore;
  }

  @Override
  public AcquisitionService acquisitionService() {
    return acquisitionService;
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

    S3Client s3Client = S3Client.builder()
        .overrideConfiguration(overrides -> overrides
            .addMetricPublisher(metrics)
            .apiCallAttemptTimeout(config.callTimeout))
        .build();

    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(config.callTimeout)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    Store<ContentDocument, UUID> contentStore = new DynamoStore<>(
        dynamoClient,
        config.contentStoreName,
        new JacksonDynamoSerializer<>(
            ContentDocument.class,
            UUID::toString,
            ContentDocument::targetId));
    Store<InvestigateDocument, UUID> investigateStore = new DynamoStore<>(
        dynamoClient,
        config.investigateStoreName,
        new JacksonDynamoSerializer<>(
            InvestigateDocument.class,
            UUID::toString,
            InvestigateDocument::targetId));
    Store<byte[], CaptureReference> sourceCaptureStore = new S3Store<>(
        s3Client,
        config.captureBucketName,
        new ByteArrayS3Serializer(config.sourceCapturePrefix));
    Store<byte[], CaptureReference> textCaptureStore = new S3Store<>(
        s3Client,
        config.captureBucketName,
        new ByteArrayS3Serializer(config.textCapturePrefix));
    MetadataFetcher metadataFetcher = new HTTPMetadataFetcher(
        httpClient,
        config.callTimeout,
        config.maxRedirects,
        config.userAgent);
    BodyFetcher bodyFetcher = new HTTPBodyFetcher(
        httpClient,
        config.callTimeout,
        config.maxRedirects,
        config.userAgent);
    AcquisitionService acquisitionService = new PersistentAcquisitionService(
        metadataFetcher,
        bodyFetcher,
        config.interpreters,
        contentStore,
        sourceCaptureStore,
        textCaptureStore);

    LetterQueue<InvestigateRequest> investigateQueue = new LetterQueue<>(
        descriptor(sqsClient, config.inQueueName),
        descriptor(sqsClient, config.inQueueName),
        descriptor(sqsClient, config.retryQueueName),
        descriptor(sqsClient, config.errorQueueName),
        InvestigateRequest.class);
    LetterQueue<AnnotateRequest> annotateQueue = new LetterQueue<>(
        descriptor(sqsClient, config.annotateQueueName),
        descriptor(sqsClient, config.annotateQueueName),
        descriptor(sqsClient, config.annotateQueueName),
        descriptor(sqsClient, config.annotateQueueName),
        AnnotateRequest.class);

    return new AWSInvestigateInfrastructure(
        metrics,
        sqsClient,
        dynamoClient,
        s3Client,
        ExecutionRouter.of(
            ExecutionRouter.bind(
                ExecutionStage.ANNOTATE,
                AnnotateRequest.class,
                annotateQueue::send)),
        investigateQueue,
        annotateQueue,
        new AttemptEventStore(dynamoClient, config.attemptEventStoreName),
        new ExecutionEventStore(dynamoClient, config.executionStoreName),
        investigateStore,
        contentStore,
        acquisitionService);
  }

  private static SQSQueueDescriptor descriptor(
      final @NonNull SqsClient sqsClient,
      final @NonNull String canonicalName) {
    return new SQSQueueDescriptor(sqsClient, resolveQueueUrl(sqsClient, canonicalName));
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
    s3Client.close();
  }
}
