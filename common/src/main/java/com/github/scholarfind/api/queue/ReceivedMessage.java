package com.github.scholarfind.api.queue;

import com.github.scholarfind.api.queue.QueueException.FatalTaskException;
import com.github.scholarfind.api.queue.QueueException.RetryTaskException;
import com.github.scholarfind.utility.Envelope;

public record ReceivedMessage(
		QueueMessage message,
		Acknowledgement acknowledgement) {

	public <T> Envelope<T> deserialize(MessageDeserializer<T> deserializer) {
		Envelope<T> envelope = null;
		try {
			envelope = new Envelope<>(deserializer.decode(message()), acknowledgement());
		} catch (RetryTaskException exception) {
			acknowledgement().retry();
		} catch (FatalTaskException exception) {
			acknowledgement().error();
		} catch (Exception exception) {
			acknowledgement().error();
		}
		return envelope;
	}
}
