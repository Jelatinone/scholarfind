package com.github.scholarfind.meta;

import java.util.List;

public sealed interface CollectionResult<Type> {
    public static record Afloat<Type>(List<Type> collection) implements CollectionResult<Type> {
    }

    public static record Alive<Type>() implements CollectionResult<Type> {
    }

    public static record Empty<Type>() implements CollectionResult<Type> {
    }
}
