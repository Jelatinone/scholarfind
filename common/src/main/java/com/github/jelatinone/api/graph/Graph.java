package com.github.jelatinone.api.graph;

public interface Graph<Vertex, Edge, Identifier> {

  void putVertex(Vertex node);

  void putEdge(Edge edge);

  GraphEdges<Edge, Identifier> edges();

  GraphVertices<Vertex, Identifier> vertices();
}
