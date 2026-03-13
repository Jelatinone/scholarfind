package com.github.scholarfind.task;

import static com.github.scholarfind.meta.result.PostResult.FAILURE_FATAL;
import static com.github.scholarfind.meta.result.PostResult.FAILURE_RETRY;
import static com.github.scholarfind.meta.result.PostResult.SUCCESS;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.INFO;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.scholarfind.api.queue.QueueResult;
import com.github.scholarfind.infra.queue.AnnotateRequestQueue;
import com.github.scholarfind.infra.queue.InvestigateRequestQueue;
import com.github.scholarfind.infra.repository.ContextDocumentStore;
import com.github.scholarfind.infra.repository.InvestigateDocumentStore;
import com.github.scholarfind.meta.ParallelTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.meta.result.CollectionResult;
import com.github.scholarfind.meta.result.OperationResult;
import com.github.scholarfind.meta.result.PostResult;
import com.github.scholarfind.meta.transitory.Disposition;
import com.github.scholarfind.meta.transitory.Emission;
import com.github.scholarfind.meta.transitory.Outcome;
import com.github.scholarfind.models.annotate.AnnotateRequest;
import com.github.scholarfind.models.investigate.Classification;
import com.github.scholarfind.models.investigate.ClassificationType;
import com.github.scholarfind.models.investigate.InvestigateDocument;
import com.github.scholarfind.models.investigate.InvestigateRequest;
import com.github.scholarfind.models.shared.ContextDocument;
import com.github.scholarfind.models.shared.DocumentHeader;
import com.github.scholarfind.models.shared.ReasonCode;
import com.github.scholarfind.models.shared.Request;
import com.github.scholarfind.models.shared.TraceReference;
import com.github.scholarfind.task.evidence.EvidenceIdentifierConfiguration;
import com.github.scholarfind.task.score.DominanceConfiguration;
import com.github.scholarfind.task.score.ScoreRuleConfiguration;
import com.github.scholarfind.task.signal.SignalCost;
import com.github.scholarfind.task.signal.SignalCostConfiguration;
import com.github.scholarfind.task.signal.SignalExtractorConfiguration;
import com.github.scholarfind.task.validation.ClassificationConfiguration;
import com.github.scholarfind.validation.Reason;
import com.github.scholarfind.validation.ValidatorPipeline;
import com.github.scholarfind.validation.ValidatorPipelineResult;
import com.github.scholarfind.validation.ValidatorResult;
import com.github.scholarfind.utility.Envelope;

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

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public final class SearchTask
    extends ParallelTask<Envelope<InvestigateRequest>, Envelope<Outcome<InvestigateDocument>>> {

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
        contextStoreName = "store_context";

    @Builder.Default
    int callTimeoutSeconds = 30;

    @Builder.Default
    ClassificationConfiguration classificationConfiguration = new ClassificationConfiguration(
        new SignalCostConfiguration(Map.of(), SignalCost.FREE), new SignalExtractorConfiguration(Set.of()),
        new EvidenceIdentifierConfiguration(List.of()), new ScoreRuleConfiguration(Map.of()),
        new DominanceConfiguration(0D, 0D));

    @Builder.Default
    ValidatorPipeline<InvestigateDocument, SearchContext> validatorPipeline = new ValidatorPipeline<>(Set.of(),
        Set.of());
  }

  static Logger logger = LoggerFactory.getLogger(SearchTask.class);

  Configuration searchConfig;
  MetricPublisher metrics;

  InvestigateRequestQueue inQueue;
  AnnotateRequestQueue outQueue;
  InvestigateDocumentStore investigateStore;
  ContextDocumentStore contextStore;

  public SearchTask(
      final Task.Configuration taskConfig,
      final @NonNull SearchTask.Configuration searchConfig,
      final @NonNull ExecutorService executor) {
    super(executor, taskConfig);
    this.searchConfig = searchConfig;

    metrics = CloudWatchMetricPublisher.builder()
        .cloudWatchClient(CloudWatchAsyncClient.create())
        .detailedMetrics(CoreMetric.API_CALL_DURATION, CoreMetric.API_CALL_SUCCESSFUL)
        .build();
    useMessage("Initialized resources : metrics publisher", INFO);

    SqsClient sqsClient = SqsClient.builder()
        .overrideConfiguration(config -> config
            .addMetricPublisher(metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(searchConfig.callTimeoutSeconds)))
        .build();
    useMessage("Initialized resources : sqs client", INFO);

    DynamoDbClient dynamoClient = DynamoDbClient.builder()
        .overrideConfiguration(config -> config
            .addMetricPublisher(metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(searchConfig.callTimeoutSeconds)))
        .build();
    useMessage("Initialized resources : dynamo client", INFO);

    String inQueueUrl = resolveQueueUrl(sqsClient, searchConfig.inQueueName);
    String outQueueUrl = resolveQueueUrl(sqsClient, searchConfig.outQueueName);
    String retryQueueUrl = resolveQueueUrl(sqsClient, searchConfig.retryQueueName);
    String errorQueueUrl = resolveQueueUrl(sqsClient, searchConfig.errorQueueName);

    inQueue = new InvestigateRequestQueue(sqsClient, inQueueUrl, retryQueueUrl, errorQueueUrl, this::useMessage);
    outQueue = new AnnotateRequestQueue(sqsClient, outQueueUrl, this::useMessage);
    investigateStore = new InvestigateDocumentStore(dynamoClient, searchConfig.investigateStoreName, this::useMessage);
    contextStore = new ContextDocumentStore(dynamoClient, searchConfig.contextStoreName, this::useMessage);
  }

  @Override
  public void close() throws IOException {
    metrics.close();
    inQueue.close();
    outQueue.close();
    investigateStore.close();
    contextStore.close();
  }

  @Override
  protected @NonNull CollectionResult<Envelope<InvestigateRequest>> collect() {
    QueueResult<InvestigateRequest> receivedMessages = inQueue.poll(_taskConfig.collectionSize);
    List<Envelope<InvestigateRequest>> envelopes = receivedMessages.messages().stream()
        .map(message -> new Envelope<>(message.message(), message.acknowledgement()))
        .toList();
    if (!envelopes.isEmpty()) {
      return new CollectionResult.Alive<>(envelopes);
    }
    return switch (receivedMessages.state()) {
      case ACTIVE, IDLE -> new CollectionResult.Idle<>();
      case EMPTY -> new CollectionResult.Empty<>();
    };
  }

  @Override
  protected OperationResult<Envelope<Outcome<InvestigateDocument>>> operate(
      final @NonNull Envelope<InvestigateRequest> operand) {
    InvestigateRequest request = operand.document();
    Instant reviewedAt = Instant.now();

    InvestigateDocument currentDocument = InvestigateDocument.builder()
        .documentHeader(new DocumentHeader(
            InvestigateDocument.schemaVersion,
            UUID.randomUUID(),
            request.requestHeader().requestId(),
            request.target().targetId(),
            reviewedAt))
        .requestHeader(request.requestHeader())
        .target(request.target())
        .trace(new TraceReference(request.target().normalizedUrl(), null, SearchTask.class.getSimpleName(),
            request.target().depth()))
        .reviewedAt(reviewedAt)
        .classification(new Classification(Map.of()))
        .confidence(0D)
        .discoveredTargetCount(0)
        .build();

    InvestigateDocument retrievedInvestigate = investigateStore.get(request.target().targetId());
    ContextDocument retrievedContext = contextStore.get(request.target().targetId());

    SearchContext pipelineContext = new SearchContext(
        currentDocument,
        searchConfig.classificationConfiguration,
        reviewedAt,
        retrievedInvestigate,
        retrievedContext);
    ValidatorPipelineResult<InvestigateDocument> pipelineResult = searchConfig.validatorPipeline
        .process(pipelineContext);

    InvestigateDocument document = pipelineResult == null ? currentDocument : pipelineResult.document();
    ValidatorResult validation = pipelineResult == null ? new ValidatorResult() : pipelineResult.result();

    List<Emission<? extends Request>> emissions = List.of();
    Disposition disposition = Disposition.COMPLETE;

    if (!validation.processable() && validation.reasons().contains(Reason.ATTEMPTS_EXCEEDED)) {
      disposition = Disposition.FAIL_PERMANENT;
    } else if (validation.processable()) {
      Map<ClassificationType, Double> contributions = document.classification() == null
          ? Map.of()
          : document.classification().contributions();
      double dominance = contributions.values().stream()
          .mapToDouble(Double::doubleValue)
          .max()
          .orElse(0D);
      List<ClassificationType> contenders = contributions.entrySet().stream()
          .filter(entry -> dominance - entry.getValue() <= searchConfig.classificationConfiguration
              .dominanceConfiguration().dominanceEpsilon())
          .map(Map.Entry::getKey)
          .toList();
      boolean boundary = contenders.stream()
          .anyMatch(contender -> contributions.get(contender) >= searchConfig.classificationConfiguration
              .dominanceConfiguration().minimumConfidence());
        if (boundary
            && !contenders.contains(ClassificationType.LANDING)
            && !contenders.contains(ClassificationType.NOT_APPLICABLE)) {
          emissions = List.of(new Emission<>(
              new AnnotateRequest(document.requestHeader(), document.target(), document.classification()),
              null,
              null,
              document.requestHeader().idempotencyKey()));
        }
    }

    Set<ReasonCode> reasonCodes = validation.reasons().stream()
        .map(reason -> (ReasonCode) reason)
        .collect(Collectors.toSet());
    Outcome<InvestigateDocument> outcome = new Outcome<>(document, disposition, reasonCodes, emissions);
    return new OperationResult<>(new Envelope<>(outcome, operand.acknowledgement()));
  }

  @Override
  protected PostResult post(final OperationResult<Envelope<Outcome<InvestigateDocument>>> result) {
    if (result == null || result.value() == null) {
      useMessage(String.format("Operation result document was null : %s", result), DEBUG);
      return FAILURE_FATAL;
    }

    Envelope<Outcome<InvestigateDocument>> envelope = result.value();
    Outcome<InvestigateDocument> outcome = envelope.document();
    InvestigateDocument document = outcome.document();

    useMessage(String.format("document disposition : %s", outcome.disposition()), DEBUG);
    try {
      investigateStore.put(document);
      switch (outcome.disposition()) {
        case COMPLETE -> {
          for (Emission<? extends Request> emission : outcome.emissions()) {
            if (emission.request() instanceof AnnotateRequest annotateRequest) {
              outQueue.send(annotateRequest);
            }
          }
          envelope.acknowledgement().success();
        }
        case RETRY -> envelope.acknowledgement().retry();
        case FAIL_PERMANENT -> envelope.acknowledgement().error();
      }
      return SUCCESS;
    } catch (final Exception exception) {
      useMessage(
          String.format("Raised fatal exception during post : %s", exception.getMessage()),
          ERROR,
          exception);
      return FAILURE_RETRY;
    }
  }

  private String resolveQueueUrl(SqsClient sqsClient, String queueName) {
    String queueUrl = sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(queueName)
            .build())
        .queueUrl();
    useMessage(String.format("Resolved resource location : %s", queueName), INFO);
    return queueUrl;
  }
}
