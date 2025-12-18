package com.github.scholarfind.meta;

/**
 * 
 * <h1>State</h1>
 * 
 * <p>
 * Describes the state of {@link Task#run() operation} for a given {@link Task
 * task} at a point during execution.
 * </p>
 * 
 * @author Cody Washington
 */
public enum State {

  CREATED,

  AWAITING,

  COLLECTING,

  RESTARTING,

  COMPLETED,

  FAILED,

  // Sequential-exclusive States

  OPERATING,

  POSTING,

  // Parallel-exlusive States

  DISPATCHING,

  WORKING,
}
