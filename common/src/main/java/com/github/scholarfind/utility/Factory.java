package com.github.scholarfind.utility;

@FunctionalInterface
public interface Factory<Create, Context> {

  /**
   * Create a given resource using a factory method from a context
   * 
   * @param context Resource context
   * @return Created resource
   */
  Create create(Context context);
}
