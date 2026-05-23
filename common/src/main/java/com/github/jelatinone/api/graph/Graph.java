package com.github.jelatinone.api.graph;

import com.github.jelatinone.model.struct.Identity;

public interface Graph<V extends Vertex<? extends Identifier>, E extends Edge<? extends Identifier, ? extends Identifier, ? extends Identifier>, Identifier extends Identity>
    extends AutoCloseable {

  void putVertex(V node);

  void putEdge(E edge);

  GraphEdges<E, Identifier> edges();

  GraphVertices<V, Identifier> vertices();
}
