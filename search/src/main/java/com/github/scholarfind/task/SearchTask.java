package com.github.scholarfind.task;

import static org.slf4j.event.Level.*;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.*;
import static com.github.scholarfind.models.DecisionType.*;
import static com.github.scholarfind.models.search.ClassificationType.*;
import static com.github.scholarfind.api.QueueHelpers.*;
import static com.github.scholarfind.api.StoreHelpers.*;
import static com.github.scholarfind.meta.Post.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scholarfind.backoff.BackoffScheduler;
import com.github.scholarfind.backoff.ExponentialBackoffScheduler;
import com.github.scholarfind.meta.CollectResult;
import com.github.scholarfind.meta.Post;
import com.github.scholarfind.meta.SequentialTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.models.DecisionType;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Trace;
import com.github.scholarfind.models.context.ContextDocument;
import com.github.scholarfind.models.search.Classification;
import com.github.scholarfind.models.search.ClassificationType;
import com.github.scholarfind.models.search.SearchDocument;
import com.github.scholarfind.task.evidence.EvidenceIdentifierConfiguration;
import com.github.scholarfind.task.evidence.EvidenceRule;
import com.github.scholarfind.task.score.ScoreRuleConfiguration;
import com.github.scholarfind.task.signal.SignalCostConfiguration;
import com.github.scholarfind.task.signal.SignalExtractor;
import com.github.scholarfind.task.signal.SignalExtractorRegistry;
import com.github.scholarfind.task.signal.SignalValue;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public final class SearchTask extends SequentialTask<SearchDocument, OperationResult<SearchDocument>> {

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
        apiCallTimeoutSeconds = 5_000L;

    @Builder.Default
    Integer apiMaximumSearchAttempts = 5;

    @Builder.Default
    Long networkTimeoutMilliseconds = 3000L;

    @Builder.Default
    Integer networkMaximumRedirects = 5;

    @Builder.Default
    Long networkMaximumTimeoutSeconds = 3_500L,
        baseNetworkTimeoutSeconds = 100L,
        networkBackoffFactor = 10001 / 100L;

    SignalExtractorRegistry extractorRegistry;
    SignalCostConfiguration costConfiguration;

    EvidenceIdentifierConfiguration evidenceConfiguration;
    ScoreRuleConfiguration scoreConfiguration;

    DecisionPolicy decisionPolicy;

  }

  static Logger _logger = LoggerFactory.getLogger(SearchTask.class);
  static MetricPublisher _metrics = CloudWatchMetricPublisher.builder()
      .cloudWatchClient(CloudWatchAsyncClient.create())
      .detailedMetrics(CoreMetric.API_CALL_DURATION)
      .build();

  ObjectMapper _mapper;
  BackoffScheduler _networkScheduler;

  SqsClient _queueClient;
  DynamoDbClient _dynamoClient;

  String _inQueueUrl,
      _outQueueUrl,
      _retryQueueUrl,
      _errorQueueUrl;

  Configuration _searchConfig;

  public SearchTask(Task.Configuration taskConfig, SearchTask.Configuration searchConfig) {
    super(taskConfig);
    _searchConfig = searchConfig;

    _mapper = new ObjectMapper()
        .configure(Feature.ALLOW_COMMENTS, true)
        .configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(Feature.ALLOW_NUMERIC_LEADING_ZEROS, true)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    _networkScheduler = new ExponentialBackoffScheduler(
        _searchConfig.baseNetworkTimeoutSeconds,
        _searchConfig.networkMaximumTimeoutSeconds,
        _searchConfig.networkBackoffFactor);

    _queueClient = SqsClient.builder()
        .overrideConfiguration(config -> config
            .addMetricPublisher(_metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(_searchConfig.apiCallTimeoutSeconds)))
        .build();
    useMessage("Initialized resources : queue client", INFO);

    _dynamoClient = DynamoDbClient.builder()
        .overrideConfiguration(config -> config
            .addMetricPublisher(_metrics)
            .apiCallAttemptTimeout(Duration.ofSeconds(_searchConfig.apiCallTimeoutSeconds)))
        .build();
    useMessage("Initialized resources : store client", INFO);

    _inQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.inQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource location : ingestion queue URL", INFO);

    _outQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.outQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource location : output queue URL", INFO);

    _retryQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.retryQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource location : retry queue URL", INFO);

    _errorQueueUrl = _queueClient.getQueueUrl(
        GetQueueUrlRequest.builder()
            .queueName(searchConfig.errorQueueName)
            .build())
        .queueUrl();
    useMessage("Resolved resource location : error queue URL", INFO);
  }

  private Classification classify(@NonNull Trace trace, ContextDocument context) {
    Map<ClassificationType, Double> scores = new HashMap<>();
    _searchConfig.costConfiguration.costs().forEach((identifier, cost) -> {
      SignalExtractor extractor = _searchConfig.extractorRegistry.extractor(identifier);
      Optional<SignalValue> value = extractor.extract(trace, context);

      List<EvidenceRule> evidence = _searchConfig.evidenceConfiguration.rulesFor(identifier);
      evidence.forEach(rule -> {
        scores.merge(
            rule.classification(),
            _searchConfig.scoreConfiguration.policyFor(rule.classification())
                .apply(rule.weight().apply(value)),
            Double::sum);
      });
    });
    Classification classification = new Classification(scores);
    return classification;
  }

  private SearchDocument parse(final @NonNull Message message) {
    Map<String, MessageAttributeValue> attributes = message.messageAttributes();
    String body = message.body();

    Long schemaVersion = Long.parseLong(attributes.get("schemaVersion").stringValue());

    SearchDocument document = null;
    if (schemaVersion == SearchDocument.schemaVersion) {
      try {
        document = SearchDocument.parse(message);
        useMessage(String.format("Parse message completed : %s", message.messageId()), INFO);
        return document;
      } catch (final MalformedURLException exception) {
        queueMessage(_queueClient, _errorQueueUrl, body, attributes, this::useMessage);
      } catch (final IOException exception) {
        queueMessage(_queueClient, _retryQueueUrl, body, attributes, this::useMessage);
      }
    }
    useMessage(
        String.format("Parse message failed : %s", message.messageId()),
        ERROR);
    return document;
  }

  @Override
  public void close() throws IOException {
    _queueClient.close();
    _dynamoClient.close();
  }

  @Override
  protected @NonNull CollectResult<@NonNull SearchDocument> collect() {
    ReceiveMessageRequest collectionRequest = ReceiveMessageRequest.builder()
        .queueUrl(_inQueueUrl)
        .maxNumberOfMessages(_taskConfig.collectionSize)
        .build();
    ReceiveMessageResponse collectionResponse = _queueClient.receiveMessage(collectionRequest);
    SdkHttpResponse collectionSdkResponse = collectionResponse.sdkHttpResponse();

    List<Message> collectionMessages;
    if (collectionSdkResponse.isSuccessful()) {
      collectionMessages = collectionResponse.messages();
    } else {
      collectionMessages = List.of();
    }
    useMessage(
        String.format("Collection results size : %d", collectionMessages.size()),
        INFO);
    useMessage(
        String.format("Collection results status : %d", collectionSdkResponse.statusCode()),
        INFO);

    GetQueueAttributesRequest attributesRequest = GetQueueAttributesRequest
        .builder()
        .queueUrl(_inQueueUrl)
        .attributeNames(APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED, APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
            APPROXIMATE_NUMBER_OF_MESSAGES)
        .build();
    GetQueueAttributesResponse attributesResponse = _queueClient.getQueueAttributes(attributesRequest);
    SdkHttpResponse attributesSdkResponse = attributesResponse.sdkHttpResponse();

    boolean queueAlive;
    if (attributesSdkResponse.isSuccessful()) {
      Map<QueueAttributeName, String> queueAttributes = attributesResponse.attributes();
      queueAlive = queueAttributes.values()
          .stream()
          .allMatch((value) -> Integer.valueOf(value) != 0);
    } else {
      queueAlive = false;
    }
    useMessage(
        String.format("Collection queue metrics results : %d", queueAlive),
        INFO);
    useMessage(
        String.format("Collection queue metrics status : %d", attributesSdkResponse.statusCode()),
        INFO);

    List<SearchDocument> searchDocuments = collectionMessages.stream()
        .map(this::parse)
        .filter(Objects::nonNull)
        .toList();
    useMessage(
        String.format("Collection valid results size : %d", searchDocuments.size()),
        INFO);

    CollectResult<SearchDocument> result;
    if (collectionMessages.size() > 0) {
      result = new CollectResult.Alive<SearchDocument>(searchDocuments);
    } else {
      result = queueAlive
          ? new CollectResult.Idle<>()
          : new CollectResult.Empty<>();
    }
    return result;
  }

  @Override
  protected @NonNull OperationResult<SearchDocument> operate(final @NonNull SearchDocument operand) {
    Header header = operand.header();
    Trace trace = operand.trace();

    UUID id = header.id();
    long schemaVersion = header.schemaVersion();

    int attempts = trace.attempt();

    ZonedDateTime discoveredAt = trace.discoveredAt();
    ZonedDateTime reviewedAt = ZonedDateTime.now();

    SearchDocument retrievedSearch = getItem(_dynamoClient, id, _searchConfig.searchStoreName,
        SearchDocument::parse, this::useMessage);
    ContextDocument retrievedContext = getItem(_dynamoClient, id, _searchConfig.contextStoreName,
        ContextDocument::parse, this::useMessage);

    boolean errorExpired = discoveredAt.plusDays(_searchConfig.apiDataExpirationDays).isBefore(reviewedAt);
    boolean errorAttempts = attempts >= _searchConfig.apiMaximumSearchAttempts;
    boolean errorSchema = schemaVersion != SearchDocument.schemaVersion;

    DecisionType decision = CONTINUE;
    if (errorSchema) {
      // In the future we'll setup some sort of migration chain...?
      decision = TOMBSTONE;
    } else if (errorAttempts) {
      decision = TOMBSTONE;
    } else if (errorExpired) {
      decision = RETRY;
    }

    // boolean shouldClassify = false;
    // if (retrievedSearch == null) {
    // shouldClassify = true;
    // } else {
    // ZonedDateTime retrievedReviewedAt = retrievedSearch.trace().reviewedAt();
    // Classification retrievedClassification = retrievedSearch.classification();
    // if (retrievedReviewedAt == null
    // ||
    // retrievedReviewedAt.isBefore(reviewedAt.minusDays(_searchConfig.apiDataExpirationDays)))
    // {
    // shouldClassify = true;
    // }
    // if (retrievedClassification == null) {
    // shouldClassify = true;
    // } else {
    // if (retrievedClassification.confidence() <
    // _searchConfig.classificationThresholds
    // .getOrDefault(retrievedClassification.type(), 0D)) {
    // shouldClassify = true;
    // }
    // }
    // }

    // Classification classification = shouldClassify ? classify(trace,
    // retrievedContext) : operand.classification();
    // if (classification.confidence() <
    // _searchConfig.classificationThresholds.getOrDefault(classification.type(),
    // 0D)) {
    // decision = IGNORE;
    // } else {
    // switch (classification.type()) {
    // case LANDING -> decision = SUPERSEDE;
    // case NOT_APPLICABLE -> decision = IGNORE;
    // default -> decision = PROCEED;
    // }
    // }

    Trace generatedTrace = new Trace(trace.url(), trace.parentUrl(), _taskConfig.name,
        trace.depth(), attempts + 1, discoveredAt, reviewedAt);
    Header generatedHeader = new Header(header.schemaVersion(), id, header.state());
    SearchDocument generatedDocument = new SearchDocument(generatedHeader, generatedTrace, null);

    return new OperationResult<SearchDocument>(generatedDocument, decision);
  }

  @Override
  protected @NonNull Post post(final OperationResult<SearchDocument> result) {
    if (result == null) {
      useMessage(
          "operation result document was null",
          DEBUG);
      return FAILURE_FATAL;
    }

    SearchDocument document = Objects.requireNonNull(result.document());
    DecisionType decision = result.decision();

    useMessage(
        String.format("document decision : %s", decision),
        DEBUG);
    try {
      String body = _mapper.writeValueAsString(document);
      Map<String, MessageAttributeValue> attributes = document.attributes();
      switch (decision) {
        case CONTINUE -> {

          queueMessage(_queueClient, _inQueueUrl, body, attributes, this::useMessage);
        }

        case PROCEED -> {

          queueMessage(_queueClient, _outQueueUrl, body, attributes, this::useMessage);
        }

        case RETRY -> {

          queueMessage(_queueClient, _retryQueueUrl, body, attributes, this::useMessage);
        }

        case TOMBSTONE -> {

          queueMessage(_queueClient, _errorQueueUrl, body, attributes, this::useMessage);
        }

        case SUPERSEDE -> {

        }

        case IGNORE -> {

        }

        default -> {
          useMessage(
              String.format("raised fatal unknown state value : %s", decision),
              ERROR);
          return FAILURE_FATAL;
        }
      }
      return SUCCESS;
    } catch (final IOException exception) {
      useMessage(
          String.format("raised fatal IO-layer exception : %s", exception.getMessage()),
          ERROR,
          exception);
      return FAILURE_RETRY;
    }
  }
}