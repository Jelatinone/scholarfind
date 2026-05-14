package com.github.jelatinone.api.graph;

import com.github.jelatinone.api.Queryable;

public interface GraphEdges<E extends Edge<Identifier>, Identifier> extends Queryable<EdgeCriteria<Identifier>, E> {
}
