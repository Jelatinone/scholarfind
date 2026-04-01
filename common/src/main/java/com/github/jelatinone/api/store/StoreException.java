package com.github.jelatinone.api.store;

public sealed class StoreException extends RuntimeException {

	protected StoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public static final class RetryTaskException extends StoreException {
		public RetryTaskException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static final class FatalTaskException extends StoreException {
		public FatalTaskException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}