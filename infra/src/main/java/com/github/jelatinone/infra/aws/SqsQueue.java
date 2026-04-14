package com.github.jelatinone.infra.aws;

import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelatinone.api.Acknowledgement;
import com.github.jelatinone.api.queue.QueueResult;
import com.github.jelatinone.api.queue.QueueState;
import com.github.jelatinone.api.queue.ReceivedMessage;
import com.github.jelatinone.api.queue.RetryableQueue;
import com.github.jelatinone.infra.aws.serial.SqsSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SqsQueue<Value> implements RetryableQueue<Value> {
	SqsClient client;
	String inputUrl;
	String retryUrl;
	String errorUrl;
	SqsSerializer<Value> serializer;

	static Logger _logger = LoggerFactory.getLogger(SqsQueue.class);

	@Override
	public QueueResult<Value> poll(int messageCount) {
		ReceiveMessageResponse response = client.receiveMessage(
				ReceiveMessageRequest.builder()
						.queueUrl(inputUrl)
						.maxNumberOfMessages(messageCount)
						.messageAttributeNames(".*")
						.build());
		logResponse("Receive message", response.sdkHttpResponse());
		List<ReceivedMessage<Value>> messages = response.messages().stream()
				.map(this::wrap)
				.filter(java.util.Objects::nonNull)
				.toList();
		return new QueueResult<>(messages, resolve());
	}

	@Override
	public void send(Value message) {
		send(inputUrl, message);
	}

	@Override
	public void sendRetry(@NonNull Value message) {
		if (retryUrl == null) {
			throw new IllegalStateException("Retry queue URL is not configured");
		}
		send(retryUrl, message);
	}

	@Override
	public void sendError(@NonNull Value message) {
		if (errorUrl == null) {
			throw new IllegalStateException("Error queue URL is not configured");
		}
		send(errorUrl, message);
	}

	public void send(String queueUrl, Value message) {
		try {
			String body = serializer.encodeBody(message);
			Map<String, String> attributes = serializer.encodeAttributes(message);
			SendMessageResponse response = client.sendMessage(builder -> builder
					.queueUrl(queueUrl)
					.messageBody(body)
					.messageAttributes(encodeAttributes(attributes)));
			logResponse("Send message", response.sdkHttpResponse());
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to encode queue message", exception);
		}
	}

	public void send(String queueUrl, String body, Map<String, MessageAttributeValue> attributes) {
		try {
			SendMessageResponse response = client.sendMessage(builder -> builder
					.queueUrl(queueUrl)
					.messageBody(body)
					.messageAttributes(attributes));
			logResponse("Send message", response.sdkHttpResponse());
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to encode queue message", exception);
		}
	}

	private ReceivedMessage<Value> wrap(Message message) {
		try {
			Value decoded = serializer.decode(message.body(), decodeAttributes(message.messageAttributes()));
			Acknowledgement acknowledgement = new Acknowledgement() {
				@Override
				public void success() {
					delete(message.receiptHandle());
				}

				@Override
				public void retry() {
					if (retryUrl != null) {
						send(retryUrl, decoded);
					}
					delete(message.receiptHandle());
				}

				@Override
				public void error() {
					if (errorUrl != null) {
						send(errorUrl, decoded);
					}
					delete(message.receiptHandle());
				}
			};
			return new ReceivedMessage<>(decoded, acknowledgement);
		} catch (Exception exception) {
			_logger.error(String.format("Decode queue message failed : %s", exception.getMessage()));
			if (errorUrl != null) {
				send(errorUrl, message.body(), message.messageAttributes());
			}
			delete(message.receiptHandle());
			return null;
		}
	}

	private void delete(String receiptHandle) {
		DeleteMessageResponse response = client.deleteMessage(builder -> builder
				.queueUrl(inputUrl)
				.receiptHandle(receiptHandle));
		logResponse("Delete message", response.sdkHttpResponse());
	}

	private QueueState resolve() {
		GetQueueAttributesResponse response = client.getQueueAttributes(builder -> builder
				.queueUrl(inputUrl)
				.attributeNames(
						APPROXIMATE_NUMBER_OF_MESSAGES,
						APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
						APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED));
		logResponse("Resolve queue state", response.sdkHttpResponse());
		boolean containsAnyMessages = response.attributes().values().stream()
				.mapToInt(Integer::parseInt)
				.anyMatch(value -> value > 0);
		return containsAnyMessages ? QueueState.IDLE : QueueState.EMPTY;
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

	private void logResponse(String action, SdkHttpResponse response) {
		if (response == null) {
			return;
		}
		String message = String.format("%s completed : [%d] %s", action, response.statusCode(), response.statusText());
		if (response.isSuccessful()) {
			_logger.info(message);
			return;
		}
		_logger.error(message);
	}

	@Override
	public void close() {
		client.close();
	}
}