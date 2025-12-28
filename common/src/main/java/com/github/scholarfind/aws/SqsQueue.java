package com.github.scholarfind.aws;

import static org.slf4j.event.Level.*;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.*;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.event.Level;

import com.github.scholarfind.api.queue.Acknowledgement;
import com.github.scholarfind.api.queue.Queue;
import com.github.scholarfind.api.queue.QueueMessage;
import com.github.scholarfind.api.queue.QueueResult;
import com.github.scholarfind.api.queue.QueueState;
import com.github.scholarfind.api.queue.ReceivedMessage;

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
public final class SqsQueue implements Queue {

	SqsClient _client;
	BiConsumer<String, Level> _logger;
	String _inputUrl,
			_retryUrl,
			_errorUrl;

	@Override
	public QueueResult poll(int messageCount) {
		ReceiveMessageResponse receiveMessageResponse = _client.receiveMessage(
				ReceiveMessageRequest.builder()
						.queueUrl(_inputUrl)
						.maxNumberOfMessages(messageCount)
						.messageAttributeNames(".*")
						.build());

		SdkHttpResponse requestSdkResponse = receiveMessageResponse.sdkHttpResponse();
		if (requestSdkResponse.isSuccessful()) {
			_logger.accept(
					String.format("Send message to queue completed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.INFO);
		} else {
			_logger.accept(
					String.format("Send message to queue failed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.ERROR);
		}

		List<ReceivedMessage> receivedMessages = receiveMessageResponse.messages().stream()
				.map(this::wrap)
				.toList();

		QueueState state = resolve();
		QueueResult queueResult = new QueueResult(receivedMessages, state);

		return queueResult;
	}

	public ReceivedMessage wrap(Message message) {
		QueueMessage queueMessage = new QueueMessage() {
			public String body() {
				return message.body();
			}

			public Map<String, MessageAttributeValue> attributes() {
				return message.messageAttributes();
			}
		};

		Acknowledgement acknowledgement = new Acknowledgement() {
			@Override
			public void success() {
				_logger.accept(
						String.format("Acknowledged as success : %s", message.receiptHandle()),
						DEBUG);
				delete(message.receiptHandle());
			}

			@Override
			public void retry() {
				_logger.accept(
						String.format("Acknowledged as retry : %s", message.receiptHandle()),
						DEBUG);
				send(_retryUrl, message.body(), message.messageAttributes());
				delete(message.receiptHandle());
			}

			@Override
			public void error() {
				_logger.accept(
						String.format("Acknowledged as error : %s", message.receiptHandle()),
						DEBUG);
				send(_errorUrl, message.body(), message.messageAttributes());
				delete(message.receiptHandle());
			}
		};

		return new ReceivedMessage(queueMessage, acknowledgement);
	}

	private QueueState resolve() {
		GetQueueAttributesResponse getQueueAttributesResponse = _client.getQueueAttributes(
				builder -> builder.queueUrl(_inputUrl)
						.attributeNames(
								APPROXIMATE_NUMBER_OF_MESSAGES,
								APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
								APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED));
		boolean queueAlive = getQueueAttributesResponse.attributes().values()
				.stream()
				.mapToInt(Integer::parseInt)
				.anyMatch(value -> value > 0);

		SdkHttpResponse requestSdkResponse = getQueueAttributesResponse.sdkHttpResponse();
		if (requestSdkResponse.isSuccessful()) {
			_logger.accept(
					String.format("Resolve queue state completed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.INFO);
		} else {
			_logger.accept(
					String.format("Delete queue state failed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.ERROR);
		}

		return queueAlive ? QueueState.IDLE : QueueState.EMPTY;
	}

	public void send(
			String queueUrl,
			String body,
			Map<String, MessageAttributeValue> attributes) {
		SendMessageResponse sendMessageResponse = _client.sendMessage(builder -> builder
				.queueUrl(queueUrl)
				.messageBody(body)
				.messageAttributes(attributes));
		SdkHttpResponse requestSdkResponse = sendMessageResponse.sdkHttpResponse();
		if (requestSdkResponse.isSuccessful()) {
			_logger.accept(
					String.format("Send message to queue completed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.INFO);
		} else {
			_logger.accept(
					String.format("Send message to queue failed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.ERROR);
		}
	}

	@Override
	public void send(String body, Map<String, MessageAttributeValue> attributes) {
		send(_inputUrl, body, attributes);
	}

	public void delete(String receiptHandle) {
		DeleteMessageResponse deleteMessageResponse = _client.deleteMessage(builder -> builder
				.queueUrl(_inputUrl)
				.receiptHandle(receiptHandle));
		SdkHttpResponse requestSdkResponse = deleteMessageResponse.sdkHttpResponse();
		if (requestSdkResponse.isSuccessful()) {
			_logger.accept(
					String.format("Delete message from queue completed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.INFO);
		} else {
			_logger.accept(
					String.format("Delete message from queue failed : [%d] %s",
							requestSdkResponse.statusCode(), requestSdkResponse.statusText()),
					Level.ERROR);
		}
	}
}
