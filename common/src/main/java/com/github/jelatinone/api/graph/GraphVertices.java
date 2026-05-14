package com.github.jelatinone.api.graph;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Queryable;

public interface GraphVertices<V extends Vertex<Identifier>, Identifier> extends Queryable<Criteria<Identifier>, V> {
}
