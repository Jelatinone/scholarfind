package com.github.scholarfind.task;

import static org.slf4j.event.Level.*;
import static com.github.scholarfind.meta.Post.*;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.scholarfind.api.queue.QueueResult;
import com.github.scholarfind.aws.DynamoStore;
import com.github.scholarfind.aws.SqsQueue;
import com.github.scholarfind.backoff.BackoffScheduler;
import com.github.scholarfind.backoff.ExponentialBackoffScheduler;
import com.github.scholarfind.meta.CollectionResult;
import com.github.scholarfind.meta.OperationResult;
import com.github.scholarfind.meta.Post;
import com.github.scholarfind.meta.SequentialTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.models.DecisionType;
import com.github.scholarfind.models.Trace;
import com.github.scholarfind.models.context.ContextDocument;
import com.github.scholarfind.models.search.ClassificationType;
import com.github.scholarfind.models.search.SearchDocument;
import com.github.scholarfind.task.evidence.EvidenceIdentifierConfiguration;
import com.github.scholarfind.task.evidence.EvidenceRule;
import com.github.scholarfind.task.score.ScoreRuleConfiguration;
import com.github.scholarfind.task.signal.SignalCost;
import com.github.scholarfind.task.signal.SignalCostConfiguration;
import com.github.scholarfind.task.signal.SignalExtractionResult;
import com.github.scholarfind.task.signal.SignalExtractor;
import com.github.scholarfind.task.signal.SignalExtractorRegistry;
import com.github.scholarfind.task.signal.SignalValue;
import com.github.scholarfind.utility.Mutable;
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

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public final class SearchTask
    extends SequentialTask<Envelope<SearchDocument>, OperationResult<Envelope<SearchDocument>>> {

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
    Long apiDataExpirationDays = 90L,
        apiCallTimeoutSeconds = 30L;

    @Builder.Default
    Integer apiMaximumSearchAttempts = 5;

    @Builder.Default
    Long networkTimeoutMilliseconds = 3000L;

    @Builder.Default
    Integer networkMaximumRedirects = 5;

    @Builder.Default
    Long networkMaximumTimeoutSeconds = 3_500L,
        baseNetworkTimeoutSeconds = 100L,
        networkBackoffFactor = 1_01L;

    @Builder.Default
    EvidenceIdentifierConfiguration evidenceConfiguration = new EvidenceIdentifierConfiguration(List.of());
    @Builder.Default
    ScoreRuleConfiguration scoreConfiguration = new ScoreRuleConfiguration(Map.of());
    @Builder.Default
    SignalCostConfiguration costConfiguration = new SignalCostConfiguration(Map.of(), SignalCost.FREE);

    @Builder.Default
    SignalExtractorRegistry extractorRegistry = new SignalExtractorRegistry(Set.of());
    @Builder.Default
    DecisionPolicy decisionPolicy = new DecisionPolicy(0D, 0D);
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
  BackoffScheduler _networkScheduler;

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
            .apiCallAttemptTimeout(Duration.ofSeconds(_searchConfig.apiCallTimeoutSeconds)))
        .build();
    useMessage("Initialized resources : sqs client", INFO);

    DynamoDbClient dynamoClient = DynamoDbClient.builder()
        .overrideConfiguration(config -> config
            .addMetricPublisher(_metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(_searchConfig.apiCallTimeoutSeconds)))
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

    _queue = new SqsQueue(sqsClient, this::useMessage, _inQueueUrl, _retryQueueUrl, _errorQueueUrl);
    _store = new DynamoStore(dynamoClient, _searchConfig.searchStoreName, this::useMessage);
    _networkScheduler = new ExponentialBackoffScheduler(
        _searchConfig.baseNetworkTimeoutSeconds,
        _searchConfig.networkMaximumTimeoutSeconds,
        _searchConfig.networkBackoffFactor);
  }

  private Map<ClassificationType, Double> classify(final @NonNull Trace trace, final ContextDocument context) {
    Map<ClassificationType, Double> contributions = new HashMap<>();
    Mutable<SignalCost> cost = new Mutable<SignalCost>(_searchConfig.costConfiguration.initial());

    _searchConfig.costConfiguration.costs().forEach((identifier, signalCost) -> {
      if (SignalCost.max(signalCost, cost.value) != cost.value) {
        useMessage(String.format("Identifier %s cost bounds exceeded : %s", identifier, signalCost), DEBUG);
        return;
      }

      SignalExtractor extractor = _searchConfig.extractorRegistry.extractor(identifier);
      SignalExtractionResult result = extractor.extract(trace, context);

      final Optional<SignalValue> value;
      switch (result) {
        case SignalExtractionResult.Both(SignalCost resultCost, Optional<SignalValue> resultValue) -> {
          value = resultValue;
          cost.value = (SignalCost.max(resultCost, cost.value));
          useMessage("Extractor returned : Both", DEBUG);
        }

        case SignalExtractionResult.Value(Optional<SignalValue> resultValue) -> {
          value = resultValue;
          useMessage("Extractor returned : Value", DEBUG);
        }

        case SignalExtractionResult.Cost(SignalCost resultCost) -> {
          value = Optional.empty();
          cost.value = (SignalCost.max(resultCost, cost.value));
          useMessage("Extractor returned : Cost", DEBUG);
        }
      }
      List<EvidenceRule> rules = _searchConfig.evidenceConfiguration.rulesFor(identifier);

      useMessage(
          String.format("Retrieved evidence rules : %d", rules.size()),
          DEBUG);

      rules.forEach(evidence -> {
        Double weight = _searchConfig.scoreConfiguration.policyFor(evidence.classification())
            .score(evidence.rule().apply(value));

        ClassificationType classification = evidence.classification();
        contributions.merge(
            classification,
            weight.isInfinite() || weight.isNaN() ? 0D : weight,
            Double::sum);

        useMessage(
            String.format("Rule %s applied weight : %d", classification, weight),
            DEBUG);
      });
    });
    return contributions;
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

    return null;
  }

  @Override
  protected Post post(final OperationResult<Envelope<SearchDocument>> result) {
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