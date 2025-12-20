package com.github.scholarfind.meta;

import java.util.List;

/**
 * 
 * <h1>Collect</h1>
 * 
 * Describes a general collection resulting from a {@link Task#collect()
 * collection} operation occuring, which may be in one of three states:
 * {@link Alive alive}, {@link Idle idle}, and {@link Empty empty}.
 * 
 * @author Cody Washington
 */
public sealed interface Collect<Type> {

    /**
     * Describes a list of data with elements
     */
    public static record Alive<Type>(List<Type> collection) implements Collect<Type> {
    }

    /**
     * Describes a list of data without elements, but expecting to receive elements
     */
    public static record Idle<Type>() implements Collect<Type> {
    }

    /**
     * Describes a list of data without elements, and not expecting to receive
     * elements
     */
    public static record Empty<Type>() implements Collect<Type> {
    }
}
