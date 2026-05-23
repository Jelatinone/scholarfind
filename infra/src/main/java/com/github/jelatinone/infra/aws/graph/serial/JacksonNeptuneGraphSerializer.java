package com.github.jelatinone.infra.aws.graph.serial;

import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.jelatinone.infra.JacksonMapper;
import com.github.jelatinone.model.Schemable;
import com.github.jelatinone.model.struct.Identity;
import com.github.jelatinone.api.graph.Edge;
import com.github.jelatinone.api.graph.Vertex;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JacksonNeptuneGraphSerializer<V extends Vertex<? extends Identity> & Schemable<? extends Identity>, E extends Edge<? extends Identity, ? extends Identity, ? extends Identity> & Schemable<? extends Identity>>
    implements NeptuneGraphSerializer<V, E> {

  Class<V> vertexType;
  Class<E> edgeType;

  @Override
  public Map<String, Object> encodeVertex(V vertex) throws Exception {
    Map<String, Object> properties = new HashMap<>(encodeProperties(vertex.properties())) {
      {
        put("id", encodeIdentifier(vertex.vertexId()));
      }
    };
    return Map.copyOf(properties);
  }

  @Override
  public V decodeVertex(Map<Object, Object> vertex) throws Exception {
    return JacksonMapper.mapper.convertValue(decodeProperties(vertex, vertexType), vertexType);
  }

  @Override
  public Map<String, Object> encodeEdge(E edge) throws Exception {
    Map<String, Object> properties = new HashMap<>(encodeProperties(edge.properties())) {
      {
        put("id", encodeIdentifier(edge.edgeId()));
        put("fromId", encodeIdentifier(edge.from()));
        put("toId", encodeIdentifier(edge.to()));
      }
    };
    return Map.copyOf(properties);
  }

  @Override
  public E decodeEdge(Map<Object, Object> edge) throws Exception {
    return JacksonMapper.mapper.convertValue(decodeProperties(edge, edgeType), edgeType);
  }

  public Object encodeIdentifier(Identity identifier) {
    return identifier == null ? null : identifier.identifier().toString();
  }

  private Map<String, Object> encodeProperties(Map<String, Object> properties) {
    Map<String, Object> encoded = new LinkedHashMap<>();
    properties.forEach((key, value) -> encoded.put(key, encodeValue(value)));
    return Map.copyOf(encoded);
  }

  private Object encodeValue(Object value) {
    return value instanceof Identity identity ? identity.identifier().toString() : value;
  }

  private Map<String, Object> decodeProperties(Map<Object, Object> element, Class<?> type) {
    Map<String, Object> properties = new LinkedHashMap<>(NeptuneGraphSerializer.properties(element));
    if (!type.isRecord()) {
      return Map.copyOf(properties);
    }
    for (RecordComponent component : type.getRecordComponents()) {
      if (!Identity.class.isAssignableFrom(component.getType())) {
        continue;
      }
      Object value = properties.get(component.getName());
      if (value != null && !(value instanceof Map<?, ?>)) {
        properties.put(component.getName(), Map.of("identifier", value.toString()));
      }
    }
    return Map.copyOf(properties);
  }
}
