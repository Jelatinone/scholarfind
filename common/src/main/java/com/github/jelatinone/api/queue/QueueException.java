package com.github.jelatinone.api.queue;

public sealed class QueueException extends RuntimeException {

	protected QueueException(String message, Throwable cause) {
		super(message, cause);
	}

	public static final class RetryQueueException extends QueueException {
		public RetryQueueException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static final class FatalQueueException extends QueueException {
		public FatalQueueException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
