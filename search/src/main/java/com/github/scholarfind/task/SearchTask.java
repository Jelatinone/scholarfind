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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
    String inQueueName = "queue_search";

    @Builder.Default
    String outQueueName = "queue_annotate";

    @Builder.Default
    String retryQueueName = "queue_retry";

    @Builder.Default
    String errorQueueName = "queue_error";

    @Builder.Default
    String searchStoreName = "store_search";

    @Builder.Default
    String contextStoreName = "store_context";

    @Builder.Default
    Long apiDataExpirationDays = 90L;

    @Builder.Default
    Long classificationExpirationDays = 45L;

    @Builder.Default
    Long apiCallTimeoutSeconds = 5_000L;

    @Builder.Default
    Long networkTimeoutMilliseconds = 3000L;

    @Builder.Default
    Long maximumNetworkTimeoutSeconds = 3_500L;

    @Builder.Default
    Long baseNetworkTimeoutSeconds = 100L;

    @Builder.Default
    Integer maximumNetworkRedirects = 5;

    @Builder.Default
    Long networkFactor = 10001 / 100L;

    @Builder.Default
    Integer maximumSearchAttempts = 5;

    @Builder.Default
    Double classifyConfidenceThreshold = 0.7D;

    @Builder.Default
    Collection<WeightedCueValue> classifyUrlCues = List.of();

    @Builder.Default
    Collection<WeightedCueValue> classifyDomainCues = List.of();

    @Builder.Default
    Collection<WeightedCueValue> classifyContentCues = List.of();

    @Builder.Default
    Map<Classification, Double> classificationFactors = Map.of();

    @Builder.Default
    Map<Classification, Double> classificationConstants = Map.of();
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
        _searchConfig.maximumNetworkTimeoutSeconds,
        _searchConfig.networkFactor);

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
    EnumMap<ClassificationType, Double> classificationWeights = new EnumMap<>(ClassificationType.class);
    classificationWeights.replaceAll((classification, weight) -> 0D);

    URL url = trace.url();

    String urlPath = url.getPath();
    _searchConfig.classifyUrlCues.stream()
        .filter((cue) -> urlPath.contains(cue.string()))
        .forEach((cue) -> {
          classificationWeights.merge(cue.classification(), cue.weight(), Double::sum);
        });

    String urlDomain = url.getHost();
    _searchConfig.classifyDomainCues.stream()
        .filter((cue) -> urlDomain.contains(cue.string()))
        .forEach((cue) -> {
          classificationWeights.merge(cue.classification(), cue.weight(), Double::sum);
        });

    if (context != null) {
      String rawContext = context.rawContext();
      _searchConfig.classifyContentCues.stream()
          .filter((cue) -> rawContext.contains(cue.string()))
          .forEach((cue) -> {
            classificationWeights.merge(cue.classification(), cue.weight(), Double::sum);
          });
    }

    try {
      URL redirectedUrl = url;
      int redirectAttempts = 0;
      while (redirectAttempts < _searchConfig.maximumNetworkRedirects) {
        HttpURLConnection connection = (HttpURLConnection) redirectedUrl.openConnection();
        connection.setRequestMethod("HEAD");
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(_searchConfig.networkTimeoutMilliseconds.intValue());
        connection.setReadTimeout(_searchConfig.networkTimeoutMilliseconds.intValue());
        connection.connect();

        String location = connection.getHeaderField("location");

        if (location != null) {
          redirectedUrl = URI.create(location).toURL();
          if (redirectedUrl != connection.getURL()) {
            redirectAttempts++;
            classificationWeights.compute(NOT_APPLICABLE,
                (classification, weight) -> weight
                    * _searchConfig.classificationFactors.getOrDefault(NOT_APPLICABLE, 1D));
            connection.disconnect();
            continue;
          }
        }

        long contentLength = connection.getContentLengthLong();
        String contentType = connection.getContentType();

        if (contentLength != -1L) {
          classificationWeights.compute(AGGREGATOR,
              (classification, weight) -> weight + contentLength / _searchConfig.classificationFactors
                  .getOrDefault(AGGREGATOR, 0D));
        }
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
          classificationWeights.compute(SCHOLARSHIP,
              (classification, weight) -> weight + contentLength / _searchConfig.classificationFactors
                  .getOrDefault(SCHOLARSHIP, 0D));
        }
        connection.disconnect();
      }
    } catch (final IOException exception) {

    }
    Classification classification = classificationWeights.entrySet().stream()
        .map((entry) -> new Classification(entry.getKey(),
            entry.getValue()
                * _searchConfig.classificationFactors.getOrDefault(entry.getKey(), 1D)
                + _searchConfig.classificationConstants.getOrDefault(entry.getKey(), 0D)))
        .reduce((first, second) -> {
          int comparedAs = Double.compare(first.confidence(), second.confidence());
          if (comparedAs > 0)
            return first;
          if (comparedAs < 0)
            return second;
          return new Classification(UNCLASSIFIED, first.confidence());
        })
        .orElse(new Classification(UNCLASSIFIED, 0D));
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
    boolean errorAttempts = attempts >= _searchConfig.maximumSearchAttempts;
    boolean errorSchema = schemaVersion != SearchDocument.schemaVersion;

    DecisionType decision = IGNORE;
    if (errorSchema) {
      // In the future we'll setup some sort of migration chain...?
      decision = TOMBSTONE;
    } else if (errorAttempts) {
      decision = TOMBSTONE;
    } else if (errorExpired) {
      decision = RETRY;
    } else {
      decision = PROCEED;
    }

    boolean shouldClassify = false;
    if (retrievedSearch == null) {
      shouldClassify = true;
    } else {
      ZonedDateTime retrievedReviewedAt = retrievedSearch.trace().reviewedAt();
      if (retrievedReviewedAt == null
          || retrievedReviewedAt.isBefore(reviewedAt.minusDays(_searchConfig.classificationExpirationDays))) {
        shouldClassify = true;
      }
      if (retrievedSearch.classification() == null) {
        shouldClassify = true;
      } else {
        double retrievedConfidence = retrievedSearch.classification().confidence();
        if (retrievedConfidence < _searchConfig.classifyConfidenceThreshold) {
          shouldClassify = true;
        }
      }
    }

    Classification classification = shouldClassify ? classify(trace, retrievedContext) : operand.classification();

    Trace generatedTrace = new Trace(trace.url(), trace.parentUrl(), _taskConfig.name,
        trace.depth(), attempts + 1, discoveredAt, reviewedAt);
    Header generatedHeader = new Header(header.schemaVersion(), id, header.state());
    SearchDocument generatedDocument = new SearchDocument(generatedHeader, generatedTrace, classification);

    return new OperationResult<SearchDocument>(generatedDocument, decision);
  }

  @Override
  protected Post post(final OperationResult<SearchDocument> result) {
    if (result == null) {
      return FAILURE_FATAL;
    }

    SearchDocument document = Objects.requireNonNull(result.document());
    DecisionType decision = result.decision();

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