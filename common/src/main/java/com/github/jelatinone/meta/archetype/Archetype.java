package com.github.jelatinone.meta.archetype;

/**
 * 
 * <h1>Archetype</h1>
 * 
 * Generic unit of task composition, which collects data, operates on it, and
 * produces a given result.
 * 
 * @author Cody Washington
 */
public interface Archetype<Consumes, Produces>
		extends Collect<Consumes>, Operate<Consumes, Produces>, Persist<Produces> {
}
