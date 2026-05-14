package com.github.jelatinone.api.graph;

public interface Graph<V extends Vertex<Identifier>, E extends Edge<Identifier>, Identifier> extends AutoCloseable {

  void putVertex(V node);

  void putEdge(E edge);

  GraphEdges<E, Identifier> edges();

  GraphVertices<V, Identifier> vertices();
}
