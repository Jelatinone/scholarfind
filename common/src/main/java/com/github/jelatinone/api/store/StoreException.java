package com.github.jelatinone.api.store;

public sealed class StoreException extends RuntimeException {

	protected StoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public static final class RetryStoreException extends StoreException {
		public RetryStoreException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static final class FatalStoreException extends StoreException {
		public FatalStoreException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}