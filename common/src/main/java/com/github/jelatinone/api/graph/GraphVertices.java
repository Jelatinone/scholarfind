package com.github.jelatinone.api.graph;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Queryable;

public interface GraphVertices<Vertex, Identifier> extends Queryable<Criteria<Identifier>, Vertex> {
}
