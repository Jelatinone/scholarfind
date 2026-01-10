package com.github.scholarfind.task;

import static org.slf4j.event.Level.*;
import static com.github.scholarfind.meta.PostResult.*;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.scholarfind.api.queue.QueueResult;
import com.github.scholarfind.aws.DynamoStore;
import com.github.scholarfind.aws.SqsQueue;
import com.github.scholarfind.meta.CollectionResult;
import com.github.scholarfind.meta.OperationResult;
import com.github.scholarfind.meta.PostResult;
import com.github.scholarfind.meta.SequentialTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.models.DecisionType;
import com.github.scholarfind.models.context.ContextDocument;
import com.github.scholarfind.models.search.ClassificationType;
import com.github.scholarfind.models.search.SearchDocument;
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
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

// TODO: Considering a change to ParallelTask?
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public final class SearchTask
    extends SequentialTask<Envelope<SearchDocument>, Envelope<SearchDocument>> {

  @Builder
  @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
  public static final class Configuration {
    @Builder.Default
    String inQueueName = "queue_search",
        outQueueName = "queue_annotate",
        retryQueueName = "queue_retry",
        errorQueueName = "queue_error";

    @Builder.Default
    String searchStoreName = "store_search",
        contextStoreName = "store_context";

    @Builder.Default
    Long callTimeoutSeconds = 30L;

    @Builder.Default
    ClassificationConfiguration classificationConfiguration = new ClassificationConfiguration(
        new SignalCostConfiguration(Map.of(), SignalCost.FREE), new SignalExtractorConfiguration(Set.of()),
        new EvidenceIdentifierConfiguration(List.of()), new ScoreRuleConfiguration(Map.of()),
        new DominanceConfiguration(0D, 0D));

    @Builder.Default
    ValidatorPipeline<SearchDocument, SearchContext> validatorPipeline = new ValidatorPipeline<SearchDocument, SearchContext>(
        Set.of(), Set.of());
  }

  static Logger _logger = LoggerFactory.getLogger(SearchTask.class);

  Configuration _searchConfig;
  MetricPublisher _metrics;

  String _inQueueUrl,
      _outQueueUrl,
      _retryQueueUrl,
      _errorQueueUrl;

  SqsQueue _queue;
  DynamoStore _store;

  public SearchTask(final Task.Configuration taskConfig, final @NonNull SearchTask.Configuration searchConfig) {
    super(taskConfig);
    _searchConfig = searchConfig;

    _metrics = CloudWatchMetricPublisher.builder()
        .cloudWatchClient(CloudWatchAsyncClient.create())
        .detailedMetrics(CoreMetric.API_CALL_DURATION, CoreMetric.API_CALL_SUCCESSFUL)
        .build();
    useMessage("Initialized resources : metrics publisher", INFO);

    SqsClient sqsClient = SqsClient.builder()
        .overrideConfiguration(config -> config
            .addMetricPublisher(_metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(_searchConfig.callTimeoutSeconds)))
        .build();
    useMessage("Initialized resources : sqs client", INFO);

    DynamoDbClient dynamoClient = DynamoDbClient.builder()
        .overrideConfiguration(config -> config
            .addMetricPublisher(_metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(_searchConfig.callTimeoutSeconds)))
        .build();
    useMessage("Initialized resources : dynamo client", INFO);

    _inQueueUrl = sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.inQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource location : ingestion queue URL", INFO);

    _outQueueUrl = sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.outQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource location : output queue URL", INFO);

    _retryQueueUrl = sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.retryQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource location : retry queue URL", INFO);

    _errorQueueUrl = sqsClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.errorQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource location : error queue URL", INFO);

    _queue = new SqsQueue(sqsClient, _inQueueUrl, _retryQueueUrl, _errorQueueUrl, this::useMessage);
    _store = new DynamoStore(dynamoClient, _searchConfig.searchStoreName, this::useMessage);
  }

  @Override
  public void close() throws IOException {
    _metrics.close();
    _queue.close();
    _store.close();
  }

  @Override
  protected @NonNull CollectionResult<Envelope<SearchDocument>> collect() {
    QueueResult receivedMessages = _queue.poll(_taskConfig.collectionSize);
    List<Envelope<SearchDocument>> documentEnvelopes = receivedMessages.messages().stream()
        .map((message) -> message.deserialize(SearchDocument::deserialize))
        .filter(Objects::nonNull)
        .toList();
    if (!documentEnvelopes.isEmpty()) {
      return new CollectionResult.Alive<>(documentEnvelopes);
    }
    return switch (receivedMessages.state()) {
      case ACTIVE, IDLE -> new CollectionResult.Idle<>();
      case EMPTY -> new CollectionResult.Empty<>();
    };
  }

  @Override
  protected OperationResult<Envelope<SearchDocument>> operate(
      final @NonNull Envelope<SearchDocument> operand) {
    SearchDocument document = operand.document();
    ZonedDateTime reviewedAt = ZonedDateTime.now();

    SearchDocument retrievedSearch = _store.get(_searchConfig.searchStoreName, document.header().id())
        .deserialize((item) -> SearchDocument.deserialize(item));
    ContextDocument retrievedContext = _store.get(_searchConfig.contextStoreName, document.header().id())
        .deserialize((item) -> ContextDocument.deserialize(item));

    SearchContext pipelineContext = new SearchContext(document, _searchConfig.classificationConfiguration, reviewedAt,
        retrievedSearch,
        retrievedContext);
    ValidatorPipelineResult<SearchDocument> pipelineResult = _searchConfig.validatorPipeline.process(pipelineContext);

    DecisionType decision = DecisionType.TOMBSTONE;

    if (pipelineResult == null || !pipelineResult.result().processable()) {
      decision = DecisionType.TOMBSTONE;

    } else {
      ValidatorResult validation = pipelineResult.result();

      if (validation.reasons().contains(Reason.ATTEMPTS_EXCEEDED)
          || validation.reasons().contains(Reason.DOCUMENT_ERROR)) {
        decision = DecisionType.IGNORE;

      } else {
        Map<ClassificationType, Double> contributions = pipelineResult.document().classification().contributions();
        double dominance = contributions.values().stream()
            .mapToDouble(Double::doubleValue).max().orElse(0D);
        List<ClassificationType> contenders = contributions.entrySet().stream()
            .filter(entry -> dominance - entry.getValue() <= _searchConfig.classificationConfiguration
                .dominanceConfiguration().dominanceEpsilon())
            .map(Map.Entry::getKey)
            .toList();
        boolean boundary = contenders.stream()
            .anyMatch(
                contender -> contributions.get(contender) >= _searchConfig.classificationConfiguration
                    .dominanceConfiguration().minimumConfidence());
        if (!boundary) {
          decision = DecisionType.IGNORE;

        } else if (contenders.contains(ClassificationType.LANDING)) {
          decision = DecisionType.SUPERSEDE;

        } else if (contenders.contains(ClassificationType.NOT_APPLICABLE)) {
          decision = DecisionType.IGNORE;

        } else {
          decision = DecisionType.PROCEED;

        }
      }
    }

    return new OperationResult<>(
        new Envelope<>(pipelineResult.document(), operand.acknowledgement()),
        decision);
  }

  @Override
  protected PostResult post(final OperationResult<Envelope<SearchDocument>> result) {
    if (result == null || result.value() == null) {
      useMessage(
          String.format("Operation result document was null : %s", result),
          DEBUG);
      return FAILURE_FATAL;
    }

    Envelope<SearchDocument> envelope = result.value();
    SearchDocument document = envelope.document();
    DecisionType decision = result.decision();

    useMessage(
        String.format("document decision : %s", decision),
        DEBUG);
    try {
      String body = document.json();
      Map<String, MessageAttributeValue> attributes = document.attribute();
      switch (decision) {
        case PROCEED -> {
          _store.put(_searchConfig.searchStoreName, document.itemize());
          _queue.send(_outQueueUrl, body, attributes);
          envelope.acknowledgement().success();
        }

        case RETRY -> {
          envelope.acknowledgement().retry();
        }

        case TOMBSTONE -> {
          _store.put(_searchConfig.searchStoreName, document.itemize());
          envelope.acknowledgement().error();
        }

        case SUPERSEDE -> {
          _store.put(_searchConfig.searchStoreName, document.itemize());
          envelope.acknowledgement().success();
        }

        case IGNORE -> {
          envelope.acknowledgement().success();
        }

        default -> {
          useMessage(
              String.format("Raised fatal unknown decision state : %s", decision),
              ERROR);
          return FAILURE_FATAL;
        }
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
}