package com.github.jelatinone.api.graph;

public sealed class GraphException extends RuntimeException {

  protected GraphException(String message, Throwable cause) {
    super(message, cause);
  }

  public static final class RetryGraphException extends GraphException {
    public RetryGraphException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static final class FatalGraphException extends GraphException {
    public FatalGraphException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}