package com.github.jelatinone.infra;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.github.jelatinone.acquisition.BodyFetcher;
import com.github.jelatinone.acquisition.ContentInterpreter;
import com.github.jelatinone.acquisition.AcquisitionService;
import com.github.jelatinone.acquisition.fetch.HTTPBodyFetcher;
import com.github.jelatinone.acquisition.fetch.HTTPMetadataFetcher;
import com.github.jelatinone.acquisition.MetadataFetcher;
import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.api.store.Store;
import com.github.jelatinone.infra.construct.StageRouter;
import com.github.jelatinone.infra.queue.AnnotateRequestQueue;
import com.github.jelatinone.infra.queue.IngestRequestQueue;
import com.github.jelatinone.infra.queue.InvestigateRequestQueue;
import com.github.jelatinone.infra.repository.AttemptEventStore;
import com.github.jelatinone.infra.repository.CaptureDocumentStore;
import com.github.jelatinone.infra.repository.ContentDocumentStore;
import com.github.jelatinone.infra.repository.InvestigateDocumentStore;
import com.github.jelatinone.infra.repository.StageExecutionRecordStore;
import com.github.jelatinone.meta.construct.Router;
import com.github.jelatinone.models.annotate.AnnotateRequest;
import com.github.jelatinone.models.audit.AttemptEvent;
import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.audit.StageExecution;
import com.github.jelatinone.models.content.ContentDocument;
import com.github.jelatinone.models.ingest.IngestRequest;
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

	RetryableQueue<StageEnvelope<InvestigateRequest>> inQueue;

	@SuppressWarnings("unused")
	Queue<StageEnvelope<AnnotateRequest>> annotateQueue;
	@SuppressWarnings("unused")
	Queue<StageEnvelope<IngestRequest>> ingestQueue;

	Store<AttemptEvent, String> eventStore;
	Store<StageExecution, String> executionStore;
	Store<InvestigateDocument, UUID> investigateStore;
	Store<ContentDocument, UUID> contentStore;
	AcquisitionService acquisitionService;

	@Builder
	@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
	public static final class Configuration {
		@Builder.Default
		String inQueueName = "queue_investigate",
				annotateQueueName = "queue_annotate",
				ingestQueueName = "queue_ingest",
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
	public RetryableQueue<StageEnvelope<InvestigateRequest>> input() {
		return inQueue;
	}

	@Override
	public Router output() {
		return router;
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

		ContentDocumentStore contentStore = new ContentDocumentStore(dynamoClient, config.contentStoreName);
		CaptureDocumentStore sourceCaptureStore = new CaptureDocumentStore(
				s3Client,
				config.captureBucketName,
				config.sourceCapturePrefix);
		CaptureDocumentStore textCaptureStore = new CaptureDocumentStore(
				s3Client,
				config.captureBucketName,
				config.textCapturePrefix);
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
		AcquisitionService acquisitionService = new AcquisitionService(
				metadataFetcher,
				bodyFetcher,
				config.interpreters,
				contentStore,
				sourceCaptureStore,
				textCaptureStore);

		IngestRequestQueue ingestRequestQueue = new IngestRequestQueue(
				sqsClient,
				resolveQueueUrl(sqsClient, config.ingestQueueName),
				null,
				null);
		InvestigateRequestQueue investigateRequestQueue = new InvestigateRequestQueue(
				sqsClient,
				resolveQueueUrl(sqsClient, config.inQueueName),
				resolveQueueUrl(sqsClient, config.retryQueueName),
				resolveQueueUrl(sqsClient, config.errorQueueName));
		AnnotateRequestQueue annotateRequestQueue = new AnnotateRequestQueue(
				sqsClient,
				resolveQueueUrl(sqsClient, config.annotateQueueName));

		return new AWSInvestigateInfrastructure(
				metrics,
				sqsClient,
				dynamoClient,
				s3Client,
				StageRouter.of(
						StageRouter.bind(
								ProcessingStage.INGEST,
								IngestRequest.class,
								ingestRequestQueue::send),
						StageRouter.bind(
								ProcessingStage.ANNOTATE,
								AnnotateRequest.class,
								annotateRequestQueue::send)),
				investigateRequestQueue,
				annotateRequestQueue,
				ingestRequestQueue,
				new AttemptEventStore(dynamoClient, config.attemptEventStoreName),
				new StageExecutionRecordStore(dynamoClient, config.executionStoreName),
				new InvestigateDocumentStore(dynamoClient, config.investigateStoreName),
				contentStore,
				acquisitionService);
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