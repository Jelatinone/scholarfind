package com.github.scholarfind.task;

import static org.slf4j.event.Level.*;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.*;
import static com.github.scholarfind.meta.Post.*;
import static com.github.scholarfind.models.DecisionType.*;
import static com.github.scholarfind.models.search.ClassificationType.*;

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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scholarfind.backoff.BackoffScheduler;
import com.github.scholarfind.backoff.ExponentialBackoffScheduler;
import com.github.scholarfind.meta.Collect;
import com.github.scholarfind.meta.Post;
import com.github.scholarfind.meta.SequentialTask;
import com.github.scholarfind.meta.Task;
import com.github.scholarfind.models.DecisionType;
import com.github.scholarfind.models.Header;
import com.github.scholarfind.models.Trace;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public final class SearchTask extends SequentialTask<SearchDocument, SearchDocument> {

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
    Long apiDataExpirationDays = 90L;

    @Builder.Default
    Long apiCallTimeoutSeconds = 5_000L;

    @Builder.Default
    Long networkTimeoutMilliseconds = 3_000L;

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

  private Classification classify(@NonNull Trace trace) {
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
            classificationWeights.compute(REDIRECT,
                (classification, weight) -> weight * _searchConfig.classificationFactors.getOrDefault(REDIRECT, 0D));
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

        // TODO: Shallow-content inspection, download 50-100KB and check

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

  private SendMessageResponse queue(@NonNull String queueUrl, @NonNull String body,
      @NonNull Map<String, MessageAttributeValue> attributes) {
    SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
        .queueUrl(queueUrl)
        .messageAttributes(attributes)
        .messageBody(body)
        .build();
    SendMessageResponse sendMessageResponse = _queueClient.sendMessage(sendMessageRequest);
    SdkHttpResponse requestSdkResponse = sendMessageResponse.sdkHttpResponse();

    String id = attributes.get("id").stringValue();
    if (requestSdkResponse.isSuccessful()) {
      useMessage(
          String.format("Queue message to queue [%s] completed : %s", queueUrl, id),
          INFO);
    } else {
      useMessage(
          String.format("Queue message to queue [%s] failed : %s", queueUrl, id),
          ERROR);
    }

    return sendMessageResponse;
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
        queue(_errorQueueUrl, body, attributes);
      } catch (final IOException exception) {
        queue(_retryQueueUrl, body, attributes);
      }
    }
    useMessage(
        String.format("Parse message failed : %s", message.messageId()),
        ERROR);
    return document;
  }

  public PutItemResponse put(final @NonNull SearchDocument document) {
    Map<String, AttributeValue> item = document.item();
    PutItemRequest putItemRequest = PutItemRequest.builder()
        .item(item)
        .tableName(_searchConfig.searchStoreName)
        .build();
    PutItemResponse putItemResponse = _dynamoClient.putItem(putItemRequest);
    SdkHttpResponse requestSdkResponse = putItemResponse.sdkHttpResponse();

    if (requestSdkResponse.isSuccessful()) {
      useMessage(
          String.format("Put item to table [%s] completed : %s", _searchConfig.searchStoreName, document.header().id()
              .toString()),
          INFO);
    } else {
      useMessage(
          String.format("Put item to table [%s] failed : %s", _searchConfig.searchStoreName, document.header().id()
              .toString()),
          ERROR);
    }

    return putItemResponse;
  }

  public DeleteItemResponse delete(final @NonNull UUID id) {
    DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
        .tableName(_searchConfig.searchStoreName)
        .key(Map.of(
            "id", AttributeValue.fromS(id.toString())))
        .build();
    DeleteItemResponse deleteItemResponse = _dynamoClient.deleteItem(deleteItemRequest);
    SdkHttpResponse requestSdkResponse = deleteItemResponse.sdkHttpResponse();

    if (requestSdkResponse.isSuccessful()) {
      useMessage(
          String.format("Delete item from table [%s] completed : %s", _searchConfig.searchStoreName, id),
          INFO);
    } else {
      useMessage(
          String.format("Delete item from table [%s] completed : %s", _searchConfig.searchStoreName, id),
          ERROR);
    }

    return deleteItemResponse;
  }

  public SearchDocument get(final @NonNull UUID id) {
    GetItemRequest getItemRequest = GetItemRequest.builder()
        .tableName(_searchConfig.searchStoreName)
        .key(Map.of(
            "id", AttributeValue.fromS(id.toString())))
        .build();
    GetItemResponse getItemResponse = _dynamoClient.getItem(getItemRequest);
    SdkHttpResponse requestSdkResponse = getItemResponse.sdkHttpResponse();

    SearchDocument document = null;
    if (requestSdkResponse.isSuccessful()) {
      Map<String, AttributeValue> item = getItemResponse.item();
      document = SearchDocument.parse(item);
      useMessage(
          String.format("Retrieve item from table [%s] completed : %s", _searchConfig.searchStoreName, id),
          INFO);
      return document;
    } else {
      useMessage(
          String.format("Retrieve item from table [%s] failed : %s", _searchConfig.searchStoreName, id),
          ERROR);
    }
    return document;
  }

  @Override
  public void close() throws IOException {
    _queueClient.close();
    _dynamoClient.close();
  }

  @Override
  protected @NonNull Collect<@NonNull SearchDocument> collect() {
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

    Collect<SearchDocument> result;
    if (collectionMessages.size() > 0) {
      result = new Collect.Alive<SearchDocument>(searchDocuments);
    } else {
      result = queueAlive
          ? new Collect.Idle<>()
          : new Collect.Empty<>();
    }
    return result;
  }

  @Override
  protected @NonNull SearchDocument operate(final @NonNull SearchDocument operand) {
    Header header = operand.header();
    Trace trace = operand.trace();

    UUID generatedId = header.id();
    Long schemaVersion = header.schemaVersion();

    URL generatedUrl = trace.url();
    URL generatedParentUrl = trace.parentUrl();

    String generatedReviewer = trace.reviewer();
    DecisionType generatedDecision = trace.decision();

    Integer generatedDepth = trace.depth();
    Integer generatedAttempt = switch (generatedDecision) {
      case NEED_ANNOTATE, ERROR_MUST_DROP, ERROR_MAX_ATTEMPTS -> trace.attempt();
      default -> trace.attempt() + 1;
    };

    ZonedDateTime generatedDiscoveredAt = trace.discoveredAt();
    ZonedDateTime generatedReviewedAt = trace.reviewedAt();

    boolean shouldClassify = false;
    ZonedDateTime reviewedTime = ZonedDateTime.now();

    if (generatedDiscoveredAt.plusDays(_searchConfig.apiDataExpirationDays).isAfter(reviewedTime)) {
      generatedDecision = DATA_OUT_OF_DATE;
    }

    if (generatedAttempt > _searchConfig.maximumSearchAttempts) {
      generatedDecision = ERROR_MAX_ATTEMPTS;
    }

    if (schemaVersion != SearchDocument.schemaVersion) {
      generatedDecision = ERROR_MUST_DROP;
    }

    switch (generatedDecision) {
      case NEED_ANNOTATE -> {
        useMessage(
            String.format("Operand misplaced : %s", operand),
            ERROR);
      }

      case NEED_SEARCH -> {
        generatedDecision = NEED_ANNOTATE;
        shouldClassify = true;
      }

      case DATA_DUPLICATE_ENTRY, DATA_OUT_OF_DATE -> {
        generatedId = UUID.fromString(generatedUrl.toString());
        generatedDecision = NEED_ANNOTATE;
        shouldClassify = true;
        useMessage(
            String.format("Operand contained poor data : %s", operand),
            INFO);
      }

      case ERROR_MAX_ATTEMPTS -> {
        useMessage(
            String.format("Operand exceeded maximum attempts : %s", operand),
            INFO);
      }

      case ERROR_MALFORMED, ERROR_MUST_RETRY -> {
        generatedDecision = NEED_SEARCH;
        shouldClassify = true;
        useMessage(
            String.format("Operand contained malformed data : %s", operand),
            INFO);
      }

      default -> {
        generatedDecision = ERROR_MUST_DROP;
        useMessage(
            String.format("Operand contained unknown state : %s", operand),
            ERROR);
      }
    }

    SearchDocument retrievedSearchDocument = get(header.id());
    if (retrievedSearchDocument != null) {

      Header retrievedSearchHeader = retrievedSearchDocument.header();
      Trace retrievedSearchTrace = retrievedSearchDocument.trace();

      if (retrievedSearchHeader.schemaVersion() != schemaVersion) {
        delete(retrievedSearchHeader.id());
      }

      if (retrievedSearchTrace.discoveredAt().plusDays(_searchConfig.apiDataExpirationDays)
          .isAfter(reviewedTime)) {
        delete(retrievedSearchHeader.id());
      }
    }

    useMessage(
        String.format("Operand should classify : %s", shouldClassify),
        INFO);

    generatedReviewer = _taskConfig.name;
    generatedReviewedAt = reviewedTime;

    Header generatedHeader = new Header(schemaVersion, generatedId);
    Trace generatedTrace = new Trace(generatedUrl, generatedParentUrl, generatedReviewer, generatedDecision,
        generatedDepth, generatedAttempt, generatedReviewedAt, generatedDiscoveredAt);
    Classification generatedClassification = shouldClassify
        ? classify(trace)
        : operand.classification();

    SearchDocument generatedDocument = new SearchDocument(generatedHeader, generatedTrace, generatedClassification);
    return generatedDocument;
  }

  @Override
  protected Post post(final SearchDocument result) {
    Post operationResult = FAILURE_FATAL;
    if (result != null) {
      try {
        Map<String, MessageAttributeValue> attributes = result.extract();
        String body = _mapper.writeValueAsString(result);

        DecisionType decision = result.trace().decision();
        switch (decision) {
          case NEED_ANNOTATE -> {
            put(result);
            queue(_outQueueUrl, body, attributes);
            useMessage(
                String.format("Operand sent to out queue : %s",
                    result),
                INFO);
          }

          case NEED_SEARCH -> {
            queue(_inQueueUrl, body, attributes);
            useMessage(
                String.format("Operand sent to in queue : %s",
                    result),
                INFO);
          }

          case NEED_STORE -> {
            put(result);
            useMessage(
                String.format("Operand stored : %s",
                    result),
                INFO);
          }

          case DATA_OUT_OF_DATE, DATA_DUPLICATE_ENTRY -> {
            delete(result.header().id());
            queue(_retryQueueUrl, body, attributes);
            useMessage(
                String.format("Operand sent to retry queue : %s",
                    result),
                INFO);
          }

          case ERROR_MALFORMED, ERROR_MAX_ATTEMPTS -> {
            put(result);
            queue(_errorQueueUrl, body, attributes);
            useMessage(
                String.format("Operand sent to error queue : %s",
                    result),
                INFO);
          }

          case ERROR_MUST_DROP -> {
            delete(result.header().id());
            queue(_errorQueueUrl, body, attributes);
            useMessage(
                String.format("Operand deleted and sent to error queue : %s",
                    result),
                INFO);
          }

          case ERROR_MUST_RETRY -> {
            queue(_retryQueueUrl, body, attributes);
            useMessage(
                String.format("Operand sent to retry queue : %s",
                    result),
                INFO);
          }

        }
        operationResult = SUCCESS;
      } catch (final JsonMappingException | JsonGenerationException exception) {
        operationResult = FAILURE_FATAL;
      } catch (final IOException exception) {
        operationResult = FAILURE_RETRY;
      }
    }
    return operationResult;
  }
}