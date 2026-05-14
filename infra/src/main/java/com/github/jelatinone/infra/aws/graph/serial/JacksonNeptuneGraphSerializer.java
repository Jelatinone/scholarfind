package com.github.jelatinone.infra.aws.graph.serial;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.model.Schemable;
import com.github.jelatinone.api.graph.Edge;
import com.github.jelatinone.api.graph.Vertex;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JacksonNeptuneGraphSerializer<V extends Vertex<?> & Schemable, E extends Edge<?> & Schemable>
    implements NeptuneGraphSerializer<V, E, UUID> {

  Class<V> vertexType;
  Class<E> edgeType;

  @Override
  public Map<String, Object> encodeVertex(V vertex) throws Exception {
    Map<String, Object> properties = new HashMap<>(vertex.properties());
    properties.put("id", encodeIdentifier(vertex.vertexId()));
    return Map.copyOf(properties);
  }

  @Override
  public V decodeVertex(Map<Object, Object> vertex) throws Exception {
    return JacksonMapper.mapper.convertValue(NeptuneGraphSerializer.properties(vertex), vertexType);
  }

  @Override
  public Map<String, Object> encodeEdge(E edge) throws Exception {
    Map<String, Object> properties = new HashMap<>(edge.properties());
    properties.put("id", encodeIdentifier(edge.edgeId()));
    properties.put("fromId", encodeIdentifier(edge.from()));
    properties.put("toId", encodeIdentifier(edge.to()));
    return Map.copyOf(properties);
  }

  @Override
  public E decodeEdge(Map<Object, Object> edge) throws Exception {
    return JacksonMapper.mapper.convertValue(NeptuneGraphSerializer.properties(edge), edgeType);
  }

  public <T> Object encodeIdentifier(T identifier) {
    return identifier == null ? null : identifier.toString();
  }
}
