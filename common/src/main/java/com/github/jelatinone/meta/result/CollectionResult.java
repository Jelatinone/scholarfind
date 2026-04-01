package com.github.jelatinone.meta.result;

import java.util.List;

import com.github.jelatinone.meta.Task;

/**
 * 
 * <h1>CollectionResult</h1>
 * 
 * Describes a general collection resulting from a {@link Task#collect()
 * collection} operation occurring, which may be in one of three states:
 * {@link Alive alive}, {@link Idle idle}, and {@link Empty empty}.
 * 
 * @author Cody Washington
 */
public sealed interface CollectionResult<Type> {

  /**
   * Describes a list of data with elements
   */
  public static record Alive<Type>(List<Type> collection) implements CollectionResult<Type> {
  }

  /**
   * Describes a list of data without elements, but expecting to receive elements
   */
  public static record Idle<Type>() implements CollectionResult<Type> {
  }

  /**
   * Describes a list of data without elements, and not expecting to receive
   * elements
   */
  public static record Empty<Type>() implements CollectionResult<Type> {
  }
}
