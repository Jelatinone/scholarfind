package com.github.scholarfind.task;

import static org.slf4j.event.Level.INFO;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

import com.github.scholarfind.infra.queue.StageEnvelopeQueue;
import com.github.scholarfind.infra.repository.AttemptEventStore;
import com.github.scholarfind.infra.repository.ContextDocumentStore;
import com.github.scholarfind.infra.repository.InvestigateDocumentStore;
import com.github.scholarfind.infra.repository.StageExecutionRecordStore;
import com.github.scholarfind.meta.PipelineTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.models.annotate.AnnotateRequest;
import com.github.scholarfind.models.audit.ProcessingStage;
import com.github.scholarfind.models.investigate.Classification;
import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.models.investigate.InvestigateRequest;
import com.github.scholarfind.models.shared.ContextDocument;
import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.RequestHeader;
import com.github.scholarfind.models.shared.StageEnvelope;
import com.github.scholarfind.models.shared.TraceReference;
import com.github.scholarfind.policy.AttemptsPolicy;
import com.github.scholarfind.policy.EmissionIntent;
import com.github.scholarfind.policy.ExpirationPolicy;
import com.github.scholarfind.policy.PolicyDecision;
import com.github.scholarfind.policy.PolicyPipeline;
import com.github.scholarfind.policy.SchemaPolicy;
import com.github.scholarfind.task.evidence.EvidenceIdentifierConfiguration;
import com.github.scholarfind.task.policy.ClassificationConfiguration;
import com.github.scholarfind.task.policy.ClassificationPolicy;
import com.github.scholarfind.task.policy.RecentClassificationReusePolicy;
import com.github.scholarfind.task.policy.SearchOutcomePolicy;
import com.github.scholarfind.task.score.DominanceConfiguration;
import com.github.scholarfind.task.score.ScoreRuleConfiguration;
import com.github.scholarfind.task.signal.SignalCost;
import com.github.scholarfind.task.signal.SignalCostConfiguration;
import com.github.scholarfind.task.signal.SignalExtractorConfiguration;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.slf4j.event.Level;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public final class SearchTask
    extends PipelineTask<InvestigateRequest, AnnotateRequest, SearchContext, SearchState, InvestigateDocument> {

  static final BiConsumer<String, Level> NOOP_LOGGER = (message, level) -> {
  };

  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  @Builder
  private static final class Resources {
    MetricPublisher metrics;
    StageEnvelopeQueue<InvestigateRequest> inQueue;
    StageEnvelopeQueue<AnnotateRequest> outQueue;
    InvestigateDocumentStore investigateStore;
    ContextDocumentStore contextStore;
    AttemptEventStore attemptEventStore;
    StageExecutionRecordStore executionStore;
  }

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
        contextStoreName = "store_context",
        attemptEventStoreName = "store_attempt_event",
        executionStoreName = "store_stage_execution";

    @Builder.Default
    int callTimeoutSeconds = 30;

    @Builder.Default
    int maxAttempts = 5;

    @Builder.Default
    int expirationDays = 30;

    @Builder.Default
    int retryDelaySeconds = 300;

    @Builder.Default
    int executionHistoryLimit = 25;

    @Builder.Default
    ClassificationConfiguration classificationConfiguration = new ClassificationConfiguration(
        new SignalCostConfiguration(Map.of(), SignalCost.FREE),
        new SignalExtractorConfiguration(Set.of()),
        new EvidenceIdentifierConfiguration(List.of()),
        new ScoreRuleConfiguration(Map.of()),
        new DominanceConfiguration(0D, 0D),
        30);

    PolicyPipeline<SearchContext, SearchState> policyPipeline;
  }

  Configuration searchConfig;
  Resources resources;

  public SearchTask(
      final Task.Configuration taskConfig,
      final @NonNull SearchTask.Configuration searchConfig,
      final @NonNull ExecutorService executor, final @NonNull Resources resources) {
    super(
        executor,
        resources.inQueue,
        taskConfig,
        PipelineTask.Configuration.<AnnotateRequest, SearchContext, SearchState>builder()
            .eventStore(resources.attemptEventStore)
            .executionStore(resources.executionStore)
            .retryDuration(Duration.ofSeconds(searchConfig.retryDelaySeconds))
            .transitionHistory(searchConfig.executionHistoryLimit)
            .processingStage(ProcessingStage.INVESTIGATE)
            .build());
    this.searchConfig = searchConfig;
    this.resources = resources;
  }

  public SearchTask(
      final Task.Configuration taskConfig,
      final @NonNull SearchTask.Configuration searchConfig,
      final @NonNull ExecutorService executor) {
    this(taskConfig, searchConfig, executor, buildResources(searchConfig));
  }

  @Override
  public void close() throws IOException {
    resources.metrics.close();
    resources.inQueue.close();
    resources.outQueue.close();
    resources.investigateStore.close();
    resources.contextStore.close();
    resources.attemptEventStore.close();
    resources.executionStore.close();
  }

  @Override
  protected SearchContext buildContext(@NonNull StageEnvelope<InvestigateRequest> input, @NonNull Instant startedAt) {
    InvestigateRequest request = input.payload();
    InvestigateDocument currentDocument = InvestigateDocument.builder()
        .documentHeader(new DocumentHeader(
            InvestigateDocument.schemaVersion,
            UUID.randomUUID(),
            request.requestHeader().requestId(),
            request.target().targetId(),
            startedAt))
        .requestHeader(request.requestHeader())
        .target(request.target())
        .trace(new TraceReference(
            request.target().normalizedUrl(),
            null,
            SearchTask.class.getSimpleName(),
            request.target().depth()))
        .reviewedAt(startedAt)
        .classification(new Classification(Map.of()))
        .confidence(0D)
        .discoveredTargetCount(0)
        .build();

    InvestigateDocument retrievedInvestigate = resources.investigateStore.get(request.target().targetId());
    ContextDocument retrievedContext = resources.contextStore.get(request.target().targetId());

    return new SearchContext(
        currentDocument,
        searchConfig.classificationConfiguration,
        startedAt,
        retrievedInvestigate,
        retrievedContext);
  }

  @Override
  protected SearchState buildState(@NonNull SearchContext context) {
    return SearchState.initial(context.reviewedAt());
  }

  @Override
  protected InvestigateDocument buildDocument(
      @NonNull StageEnvelope<InvestigateRequest> input,
      @NonNull SearchContext context,
      @NonNull PolicyDecision<SearchState> decision,
      @NonNull Instant occurredAt) {
    return new InvestigateDocument(
        context.document().documentHeader(),
        context.document().requestHeader(),
        context.document().target(),
        context.document().trace(),
        occurredAt,
        decision.state() == null ? context.document().classification() : decision.state().classification(),
        decision.state() == null ? context.document().confidence() : decision.state().confidence(),
        decision.state() == null ? context.document().discoveredTargetCount()
            : decision.state().discoveredTargetCount());
  }

  @Override
  protected void persistDocument(@NonNull InvestigateDocument document) {
    resources.investigateStore.put(document);
  }

  @Override
  protected StageEnvelope<AnnotateRequest> buildEnvelope(
      @NonNull EmissionIntent<? extends AnnotateRequest> emission,
      @NonNull StageEnvelope<InvestigateRequest> input,
      @NonNull SearchContext context,
      @NonNull PolicyDecision<SearchState> decision,
      @NonNull InvestigateDocument document) {
    return StageEnvelope.of(
        ProcessingStage.ANNOTATE,
        document.documentHeader().documentId().toString(),
        emission.request());
  }

  private static Resources buildResources(final Configuration config) {
    MetricPublisher metrics = CloudWatchMetricPublisher.builder()
        .cloudWatchClient(CloudWatchAsyncClient.create())
        .detailedMetrics(CoreMetric.API_CALL_DURATION, CoreMetric.API_CALL_SUCCESSFUL)
        .build();

    SqsClient sqsClient = SqsClient.builder()
        .overrideConfiguration(overrides -> overrides
            .addMetricPublisher(metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(config.callTimeoutSeconds)))
        .build();

    DynamoDbClient dynamoClient = DynamoDbClient.builder()
        .overrideConfiguration(overrides -> overrides
            .addMetricPublisher(metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(config.callTimeoutSeconds)))
        .build();

    String inQueueUrl = resolveQueueUrl(sqsClient, config.inQueueName);
    String outQueueUrl = resolveQueueUrl(sqsClient, config.outQueueName);
    String retryQueueUrl = resolveQueueUrl(sqsClient, config.retryQueueName);
    String errorQueueUrl = resolveQueueUrl(sqsClient, config.errorQueueName);

    Resources resources = Resources.builder()
        .metrics(metrics)
        .inQueue(new StageEnvelopeQueue<>(sqsClient, inQueueUrl, retryQueueUrl, errorQueueUrl, InvestigateRequest.class,
            NOOP_LOGGER))
        .outQueue(new StageEnvelopeQueue<>(sqsClient, outQueueUrl, null, null, AnnotateRequest.class, NOOP_LOGGER))
        .investigateStore(new InvestigateDocumentStore(dynamoClient, config.investigateStoreName, NOOP_LOGGER))
        .contextStore(new ContextDocumentStore(dynamoClient, config.contextStoreName, NOOP_LOGGER))
        .attemptEventStore(new AttemptEventStore(dynamoClient, config.attemptEventStoreName, NOOP_LOGGER))
        .executionStore(new StageExecutionRecordStore(dynamoClient, config.executionStoreName, NOOP_LOGGER))
        .build();
    return resources;
  }

  @Override
  protected void emit(@NonNull StageEnvelope<AnnotateRequest> envelope) {
    resources.outQueue.send(envelope);
  }

  @Override
  protected InvestigateRequest buildRequest(InvestigateRequest request, RequestHeader nextHeader) {
    return new InvestigateRequest(nextHeader, request.target());
  }

  private static String resolveQueueUrl(SqsClient sqsClient, String queueName) {
    String queueUrl = sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(queueName)
            .build())
        .queueUrl();
    return queueUrl;
  }
}
