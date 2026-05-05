package com.github.jelatinone.meta.archetype;

@FunctionalInterface
public interface Operate<Consumes, Produces> {

  /**
   * Performs an operation on `consumable` data and maps to a `producible` a
   * result.
   * 
   * @param operand Data to be mapped
   * @return Mapped result
   */
  Produces operate(Consumes operand);
}
