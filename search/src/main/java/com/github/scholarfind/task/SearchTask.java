package com.github.scholarfind.task;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.event.Level;

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
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class SearchTask
    extends PipelineTask<InvestigateRequest, AnnotateRequest, SearchContext, SearchState, InvestigateDocument> {

  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  private static final class Resources {
    MetricPublisher metrics;

    SqsClient sqsClient;
    DynamoDbClient dynamoClient;

    String inQueueUrl;
    String outQueueUrl;
    String retryQueueUrl;
    String errorQueueUrl;
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
    Duration callTimeout = Duration.ofSeconds(30);

    @Builder.Default
    Duration retryTimeout = Duration.ofMinutes(5);

    @Builder.Default
    int maxAttempts = 5;

    @Builder.Default
    int expirationDays = 30;

    @Builder.Default
    int transitionHistory = 25;

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

  Configuration _searchConfig;
  Resources _resources;

  InvestigateDocumentStore _investigateStore;
  ContextDocumentStore _contextStore;

  public SearchTask(
      final @NonNull Task.Configuration taskConfig,
      final @NonNull SearchTask.Configuration searchConfig,
      final @NonNull ExecutorService executor) {
    this(taskConfig, searchConfig, executor, buildResources(searchConfig));
  }

  private SearchTask(
      final @NonNull Task.Configuration taskConfig,
      final @NonNull SearchTask.Configuration searchConfig,
      final @NonNull ExecutorService executor,
      final @NonNull Resources resources) {
    super(
        executor,
        taskConfig,
        PipelineTask.Configuration
            .<InvestigateRequest, AnnotateRequest, SearchContext, SearchState, InvestigateDocument>builder()
            .policyPipeline(new PolicyPipeline<>(List.of(
                new SchemaPolicy<InvestigateDocument, SearchContext, SearchState>(InvestigateDocument.schemaVersion),
                new AttemptsPolicy<InvestigateDocument, SearchContext, SearchState>(searchConfig.maxAttempts),
                new ExpirationPolicy<InvestigateDocument, SearchContext, SearchState>(searchConfig.expirationDays),
                new RecentClassificationReusePolicy(),
                new ClassificationPolicy(),
                new SearchOutcomePolicy(searchConfig.classificationConfiguration))))
            .inQueueFactory(runtime -> {
              var inQueue = new StageEnvelopeQueue<>(
                  resources.sqsClient,
                  resources.inQueueUrl,
                  resources.retryQueueUrl,
                  resources.errorQueueUrl,
                  InvestigateRequest.class,
                  runtime::useMessage);
              runtime.useMessage("Resolved resource : ingestion queue", Level.INFO);
              return inQueue;
            })
            .outQueueFactory(runtime -> {
              var outQueue = new StageEnvelopeQueue<>(
                  resources.sqsClient,
                  resources.outQueueUrl,
                  null,
                  null,
                  AnnotateRequest.class,
                  runtime::useMessage);
              runtime.useMessage("Resolved resource : emmission queue", Level.INFO);
              return outQueue;
            })
            .eventStoreFactory(runtime -> {
              var eventStore = new AttemptEventStore(
                  resources.dynamoClient,
                  searchConfig.attemptEventStoreName,
                  runtime::useMessage);
              runtime.useMessage("Resolved resource : attempt event store", Level.INFO);
              return eventStore;
            })
            .executionStoreFactory(runtime -> {
              var executionStore = new StageExecutionRecordStore(
                  resources.dynamoClient,
                  searchConfig.executionStoreName,
                  runtime::useMessage);
              runtime.useMessage("Resolved resource : execution store", Level.INFO);
              return executionStore;
            })
            .retryDuration(searchConfig.retryTimeout)
            .transitionHistory(searchConfig.transitionHistory)
            .processingStage(ProcessingStage.INVESTIGATE)
            .build());
    _searchConfig = searchConfig;
    _resources = resources;
    _investigateStore = new InvestigateDocumentStore(
        resources.dynamoClient,
        searchConfig.investigateStoreName,
        this::useMessage);
    _contextStore = new ContextDocumentStore(
        resources.dynamoClient,
        searchConfig.contextStoreName,
        this::useMessage);
  }

  @Override
  public void close() throws IOException {
    try {
      _resources.metrics.close();
      _resources.sqsClient.close();
      _resources.dynamoClient.close();
    } catch (Exception exception) {
      throw new IOException("Failed to close SearchTask resources", exception);
    }
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

    InvestigateDocument retrievedInvestigate = _investigateStore.get(request.target().targetId());
    ContextDocument retrievedContext = _contextStore.get(request.target().targetId());

    return new SearchContext(
        currentDocument,
        _searchConfig.classificationConfiguration,
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
        decision.state() == null
            ? context.document().classification()
            : decision.state().classification(),
        decision.state() == null
            ? context.document().confidence()
            : decision.state().confidence(),
        decision.state() == null
            ? context.document().discoveredTargetCount()
            : decision.state().discoveredTargetCount());
  }

  @Override
  protected void persistDocument(@NonNull InvestigateDocument document) {
    _investigateStore.put(document);
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

  @Override
  protected InvestigateRequest buildRequest(InvestigateRequest request, RequestHeader nextHeader) {
    return new InvestigateRequest(nextHeader, request.target());
  }

  private static Resources buildResources(final Configuration config) {
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

    Resources resources = Resources.builder()
        .metrics(metrics)
        .sqsClient(sqsClient)
        .dynamoClient(dynamoClient)
        .inQueueUrl(resolveQueueUrl(sqsClient, config.inQueueName))
        .outQueueUrl(resolveQueueUrl(sqsClient, config.outQueueName))
        .retryQueueUrl(resolveQueueUrl(sqsClient, config.retryQueueName))
        .errorQueueUrl(resolveQueueUrl(sqsClient, config.errorQueueName))
        .build();

    return resources;
  }

  private static String resolveQueueUrl(final @NonNull SqsClient sqsClient, final @NonNull String canonicalName) {
    return sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(canonicalName)
            .build())
        .queueUrl();
  }
}
