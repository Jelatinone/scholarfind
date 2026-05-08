package com.github.jelatinone.infra.aws.queue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelatinone.api.Query.Count;
import com.github.jelatinone.api.Query.Exists;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.queue.Queue;
import com.github.jelatinone.api.queue.QueueCriteria;
import com.github.jelatinone.api.queue.QueueException;
import com.github.jelatinone.infra.aws.queue.serial.SQSSerializer;

import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SQSQueue<Value, Queryable> implements Queue<Value, Queryable> {
	SqsClient client;
	String queueUrl;

	SQSSerializer<Value> serializer;

	static Logger _logger = LoggerFactory.getLogger(SQSQueue.class);

	@Override
	public void queue(@NonNull Value message) {
		send(message);
	}

	private void delete(String receiptHandle) {
		DeleteMessageResponse response = client.deleteMessage(builder -> builder
				.queueUrl(queueUrl)
				.receiptHandle(receiptHandle));
		response("Delete message", response.sdkHttpResponse());
	}

	@Override
	public boolean query(Exists<QueueCriteria<Queryable>> query) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'query'");
	}

	@Override
	public long query(Count<QueueCriteria<Queryable>> query) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'query'");
	}

	@Override
	public Value query(Singular<QueueCriteria<Queryable>> query) {
		try {
			ReceiveMessageResponse response = client.receiveMessage(
					ReceiveMessageRequest.builder()
							.queueUrl(queueUrl)
							.maxNumberOfMessages(1)
							.messageAttributeNames(".*")
							.build());
			response("receive message", response.sdkHttpResponse());

			Message message = response.messages().stream()
					.filter(java.util.Objects::nonNull)
					.findFirst()
					.orElse(null);
			Value envelope = serializer.decode(message.body(), decodeAttributes(message.messageAttributes()));

			delete(message.receiptHandle());
			return envelope;
		} catch (IOException exception) {
			_logger.error(String.format("Decode queue message failed : %s", exception.getMessage()));
			throw new QueueException.FatalQueueException(exception.getMessage(), exception);
		} catch (Exception exception) {
			_logger.error(String.format("Queue query failed : %s", exception.getMessage()));
			throw new QueueException.RetryQueueException(exception.getMessage(), exception);
		}
	}

	@Override
	public Collection<Value> query(Several<QueueCriteria<Queryable>> query) {
		try {
			ReceiveMessageResponse response = client.receiveMessage(
					ReceiveMessageRequest.builder()
							.queueUrl(queueUrl)
							.maxNumberOfMessages(1)
							.messageAttributeNames(".*")
							.build());
			response("receive message", response.sdkHttpResponse());

			List<Value> envelopes = response.messages().stream()
					.filter(java.util.Objects::nonNull)
					.map((message) -> {
						Value decoded;
						try {
							decoded = serializer.decode(message.body(), decodeAttributes(message.messageAttributes()));

							delete(message.receiptHandle());
							return decoded;
						} catch (IOException exception) {
							_logger.error(String.format("Decode queue message failed : %s", exception.getMessage()));
							throw new QueueException.FatalQueueException(exception.getMessage(), exception);
						}
					}).toList();
			return envelopes;
		} catch (Exception exception) {
			_logger.error(String.format("Queue query failed : %s", exception.getMessage()));
			throw new QueueException.RetryQueueException(exception.getMessage(), exception);
		}
	}

	public void send(Value message) {
		if (queueUrl == null) {
			throw new IllegalStateException("Error queue URL is not configured");
		}
		try {
			String body = serializer.encodeBody(message);
			Map<String, String> attributes = serializer.encodeAttributes(message);
			SendMessageResponse response = client.sendMessage(builder -> builder
					.queueUrl(queueUrl)
					.messageBody(body)
					.messageAttributes(encodeAttributes(attributes)));
			response("send message", response.sdkHttpResponse());
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to encode queue message", exception);
		}
	}

	public void send(String body, Map<String, MessageAttributeValue> attributes) {
		if (queueUrl == null) {
			throw new IllegalStateException("Error queue URL is not configured");
		}
		try {
			SendMessageResponse response = client.sendMessage(builder -> builder
					.queueUrl(queueUrl)
					.messageBody(body)
					.messageAttributes(attributes));
			response("send message", response.sdkHttpResponse());
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to encode queue message", exception);
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

	@Override
	public void close() {
		client.close();
	}

	private static void response(String action, SdkHttpResponse response) {
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
}
