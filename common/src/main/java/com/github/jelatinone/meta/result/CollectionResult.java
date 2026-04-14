package com.github.jelatinone.meta.result;

import java.util.List;

import com.github.jelatinone.meta.Task;

/**
 * 
 * <h1>CollectionResult</h1>
 * 
 * Describes a general collection resulting from a {@link Task#collect()}
 * collection} operation occurring, which may be in one of three states:
 * {@link Alive alive}, {@link Idle idle}, and {@link Empty empty}.
 * 
 * @author Cody Washington
 */
public sealed interface CollectionResult<Any> {

  /**
   * Describes a list of data with elements
   */
   record Alive<Any>(List<Any> collection) implements CollectionResult<Any> {
  }

  /**
   * Describes a list of data without elements, but expecting to receive elements
   */
    record Idle<Any>() implements CollectionResult<Any> {
  }

  /**
   * Describes a list of data without elements, and not expecting to receive
   * elements
   */
   record Empty<Any>() implements CollectionResult<Any> {
  }
}
