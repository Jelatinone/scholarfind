package com.github.jelatinone.infra.aws.graph.serial;

import java.util.LinkedHashMap;
import java.util.Map;

public interface NeptuneGraphSerializer<Vertex, Edge, Identifier> {

  String vertexLabel(Vertex vertex);

  Identifier vertexIdentifier(Vertex vertex);

  Map<String, Object> encodeVertex(Vertex vertex) throws Exception;

  Vertex decodeVertex(Map<Object, Object> vertex) throws Exception;

  String edgeLabel(Edge edge);

  Identifier edgeIdentifier(Edge edge);

  Identifier edgeFrom(Edge edge);

  Identifier edgeTo(Edge edge);

  Map<String, Object> encodeEdge(Edge edge) throws Exception;

  Edge decodeEdge(Map<Object, Object> edge) throws Exception;

  default Object encodeIdentifier(Identifier identifier) {
    return identifier == null ? null : identifier.toString();
  }

  public static Object property(Map<Object, Object> element, String name) {
    if (element == null || name == null) {
      return null;
    }
    return element.get(name);
  }

  public static Map<String, Object> properties(Map<Object, Object> element) {
    if (element == null) {
      return Map.of();
    }
    Map<String, Object> properties = new LinkedHashMap<>();
    element.forEach((key, value) -> {
      if (key instanceof String property) {
        properties.put(property, value);
      }
    });
    return Map.copyOf(properties);
  }
}
