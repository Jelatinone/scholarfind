package com.github.jelatinone.utility.scheduler;

/**
 * 
 * <h1>BackoffScheduler</h1>
 * 
 * <p>
 * Computes a backoff patern for a given operation.
 * </p>
 * 
 * @author Cody Washington
 */
public interface BackoffScheduler {

  /**
   * Computes the next backoff time value using the backoff pattern.
   * 
   * @return computed backoff time
   */
  long compute();

  /**
   * Computes the next backoff time value using the backoff pattern at a specific
   * step.
   * 
   * @param step computation step to compute at
   * @return computed backoff time
   */
  long compute(int step);

  /**
   * Resets the internal backoff counter to a base value
   * 
   * @return previous backoff time
   */
  long reset();

  /**
   * Acquires the current backoff time value without {@link #compute()
   * re-computing} the value.
   * 
   * @return previous backoff time
   */
  long acquire();

}
