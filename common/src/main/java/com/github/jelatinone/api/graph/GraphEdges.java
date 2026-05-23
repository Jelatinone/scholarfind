package com.github.jelatinone.api.graph;

import com.github.jelatinone.api.Queryable;
import com.github.jelatinone.model.struct.Identity;

public interface GraphEdges<E extends Edge<? extends Identifier, ? extends Identifier, ? extends Identifier>, Identifier extends Identity>
    extends Queryable<EdgeCriteria<? extends Identifier>, E> {
}
