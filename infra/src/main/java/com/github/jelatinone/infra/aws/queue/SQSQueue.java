package com.github.jelatinone.infra.aws.queue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelatinone.api.Acknowledgement;
import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query.Count;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.api.queue.QueueException;
import com.github.jelatinone.infra.aws.AWSInfrastructure;
import com.github.jelatinone.infra.aws.queue.serial.SQSSerializer;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SQSQueue<Value>
		implements Queue<String, Value, SQSCriteria>, AWSInfrastructure<CreateQueueRequest, CreateQueueResponse> {

	SqsClient client;
	SQSSerializer<Value> serializer;

	static Logger _logger = LoggerFactory.getLogger(SQSQueue.class);

	@Override
	public Optional<CreateQueueResponse> tryCreate(CreateQueueRequest create) {
		try {
			CreateQueueResponse response = client.createQueue(create);
			response("create queue", response.sdkHttpResponse());
			return Optional.of(response);
		} catch (QueueNameExistsException exception) {
			return Optional.empty();
		} catch (Exception exception) {
			throw new QueueException.RetryQueueException(exception.getMessage(), exception);
		}
	}

	@Override
	public void queue(@NonNull SQSCriteria criteria, @NonNull Value message) {
		try {
			SQSCriteria.Send send = send(criteria);
			String queueUrl = identifier(send);
			String body = serializer.encodeBody(message);
			Map<String, String> attributes = serializer.encodeAttributes(message);
			SendMessageResponse response = client.sendMessage(builder -> builder
					.queueUrl(queueUrl)
					.messageBody(body)
					.applyMutation(extra -> {
						send.deduplicationId().ifPresent(extra::messageDeduplicationId);
						send.groupId().ifPresent(extra::messageGroupId);
					})
					.messageSystemAttributes(send.messageSystemAttributes())
					.messageAttributes(encodeAttributes(attributes)));
			response("send message", response.sdkHttpResponse());
		} catch (QueueException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new QueueException.FatalQueueException("Failed to encode queue message", exception);
		} catch (Exception exception) {
			throw new QueueException.RetryQueueException("Failed to send queue message", exception);
		}
	}

	private void delete(String queueUrl, String receiptHandle) {
		DeleteMessageResponse response = client.deleteMessage(builder -> builder
				.queueUrl(queueUrl)
				.receiptHandle(receiptHandle));
		response("delete message", response.sdkHttpResponse());
	}

	@Override
	public Optional<Long> query(Count<SQSCriteria> query) {
		try {
			String queueUrl = identifier(query.criteria());
			GetQueueAttributesResponse response = client.getQueueAttributes(builder -> builder
					.queueUrl(queueUrl)
					.attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));

			response("get queue attributes", response.sdkHttpResponse());
			long count = Long
					.parseLong(response.attributes().getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
							"0"));
			return Optional.of(count);
		} catch (Exception exception) {
			_logger.error(String.format("Queue query failed : %s",
					exception.getMessage()));
			throw new QueueException.RetryQueueException(exception.getMessage(),
					exception);
		}
	}

	@Override
	public Optional<QueueEnvelope<Value>> query(Singular<SQSCriteria> query) {
		try {
			SQSCriteria.Receive receive = receive(query.criteria());
			String queueUrl = identifier(receive);
			ReceiveMessageResponse response = client.receiveMessage(
					builder -> builder
							.queueUrl(queueUrl)
							.messageAttributeNames(receive.messageAttributeNames())
							.messageSystemAttributeNames(receive.messageSystemAttributeNames())
							.maxNumberOfMessages(1)
							.build());
			response("receive message", response.sdkHttpResponse());

			Message message = response.messages().stream()
					.filter(java.util.Objects::nonNull)
					.findFirst()
					.orElse(null);
			if (message == null) {
				return Optional.empty();
			}

			Value value = serializer.decode(message.body(),
					decodeAttributes(message.messageAttributes()));
			QueueEnvelope<Value> envelope = wrap(value, queueUrl, message.receiptHandle());
			return Optional.ofNullable(envelope);
		} catch (QueueException exception) {
			throw exception;
		} catch (IOException exception) {
			_logger.error(String.format("Decode queue message failed : %s",
					exception.getMessage()));
			throw new QueueException.FatalQueueException(exception.getMessage(),
					exception);
		} catch (Exception exception) {
			_logger.error(String.format("Queue query failed : %s",
					exception.getMessage()));
			throw new QueueException.RetryQueueException(exception.getMessage(),
					exception);
		}
	}

	@Override
	public Collection<QueueEnvelope<Value>> query(Several<SQSCriteria> query) {
		try {
			SQSCriteria.Receive receive = receive(query.criteria());
			String queueUrl = identifier(receive);
			ReceiveMessageResponse response = client.receiveMessage(
					builder -> builder
							.queueUrl(queueUrl)
							.messageAttributeNames(receive.messageAttributeNames())
							.messageSystemAttributeNames(receive.messageSystemAttributeNames())
							.maxNumberOfMessages(query.limit())
							.build());
			response("receive message(s)", response.sdkHttpResponse());

			List<QueueEnvelope<Value>> envelopes = response.messages().stream()
					.filter(java.util.Objects::nonNull)
					.map((message) -> {
						Value value;
						try {
							value = serializer.decode(message.body(),
									decodeAttributes(message.messageAttributes()));
							QueueEnvelope<Value> envelope = wrap(value, queueUrl, message.receiptHandle());
							return envelope;
						} catch (IOException exception) {
							_logger.error(String.format("Decode queue message failed : %s",
									exception.getMessage()));
							throw new QueueException.FatalQueueException(exception.getMessage(),
									exception);
						}
					}).toList();
			return envelopes;
		} catch (QueueException exception) {
			throw exception;
		} catch (Exception exception) {
			_logger.error(String.format("Queue query failed : %s",
					exception.getMessage()));
			throw new QueueException.RetryQueueException(exception.getMessage(),
					exception);
		}
	}

	private Map<String, String> decodeAttributes(Map<String, MessageAttributeValue> attributes) {
		return attributes.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stringValue()));
	}

	private Map<String, MessageAttributeValue> encodeAttributes(Map<String, String> attributes) {
		return attributes.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> MessageAttributeValue.builder().dataType("String").stringValue(entry.getValue()).build()));
	}

	private QueueEnvelope<Value> wrap(Value value, String queueUrl, String receiptHandle) {
		return new QueueEnvelope<Value>(value, new Acknowledgement() {

			@Override
			public void success() {
				delete(queueUrl, receiptHandle);
			}

			@Override
			public void retry() {
				// Do nothing ;)
			}

			@Override
			public void error() {
				delete(queueUrl, receiptHandle);
			}
		});
	}

	@Override
	public void close() {
		client.close();
	}

	private static String identifier(Criteria<String> criteria) {
		return criteria.identifier().orElseThrow();
	}

	private static SQSCriteria.Send send(SQSCriteria criteria) {
		return switch (criteria) {
			case SQSCriteria.Send send -> send;
			case SQSCriteria.Location location -> new SQSCriteria.Send(
					location.identifier(),
					location.duration(),
					Optional.empty(),
					Optional.empty(),
					Map.of());
			case SQSCriteria.Receive receive -> throw new QueueException.FatalQueueException(
					"Receive criteria cannot be used to send SQS messages",
					null);
		};
	}

	private static SQSCriteria.Receive receive(SQSCriteria criteria) {
		return switch (criteria) {
			case SQSCriteria.Receive receive -> receive;
			case SQSCriteria.Location location -> new SQSCriteria.Receive(
					location.identifier(),
					location.duration(),
					List.of(),
					List.of());
			case SQSCriteria.Send send -> throw new QueueException.FatalQueueException(
					"Send criteria cannot be used to receive SQS messages",
					null);
		};
	}

	private static void response(String action, SdkHttpResponse response) {
		if (response == null) {
			return;
		}
		String message = String.format("%s [%d] : %s", action, response.statusCode(), response.statusText());
		if (response.isSuccessful()) {
			_logger.info(message);
			return;
		}
		_logger.error(message);
	}
}
