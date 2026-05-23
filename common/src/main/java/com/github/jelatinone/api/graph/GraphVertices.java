package com.github.jelatinone.api.graph;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Queryable;
import com.github.jelatinone.model.struct.Identity;

public interface GraphVertices<V extends Vertex<? extends Identifier>, Identifier extends Identity>
    extends Queryable<Criteria<? extends Identifier>, V> {
}
