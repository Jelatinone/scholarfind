package com.github.jelatinone.api.queue;

public sealed class QueueException extends RuntimeException {

	protected QueueException(String message, Throwable cause) {
		super(message, cause);
	}

	public static final class RetryTaskException extends QueueException {
		public RetryTaskException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static final class FatalTaskException extends QueueException {
		public FatalTaskException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
