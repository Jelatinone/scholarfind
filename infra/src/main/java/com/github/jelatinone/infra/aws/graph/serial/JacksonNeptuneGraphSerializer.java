package com.github.jelatinone.infra.aws.graph.serial;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.github.jelatinone.infra.JacksonMapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JacksonNeptuneGraphSerializer<Vertex, Edge, Identifier>
    implements NeptuneGraphSerializer<Vertex, Edge, Identifier> {

  Class<Vertex> vertexType;
  Class<Edge> edgeType;

  String vertexLabel;
  String edgeLabel;

  Function<Vertex, Identifier> vertexIdentifier;
  Function<Edge, Identifier> edgeIdentifier;
  Function<Edge, Identifier> edgeFrom;
  Function<Edge, Identifier> edgeTo;
  Function<Identifier, String> identifierEncoder;

  @Builder.Default
  Map<String, Function<Vertex, ?>> vertexProperties = Map.of();

  @Builder.Default
  Map<String, Function<Edge, ?>> edgeProperties = Map.of();

  @Override
  public String vertexLabel(Vertex vertex) {
    return vertexLabel;
  }

  @Override
  public Identifier vertexIdentifier(Vertex vertex) {
    return vertexIdentifier.apply(vertex);
  }

  @Override
  public Map<String, Object> encodeVertex(Vertex vertex) throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put("id", encodeIdentifier(vertexIdentifier(vertex)));
    properties.put("payload", JacksonMapper.mapper.writeValueAsString(vertex));
    vertexProperties.forEach((name, value) -> {
      Object property = value.apply(vertex);
      if (property != null) {
        properties.put(name, property.toString());
      }
    });
    return Map.copyOf(properties);
  }

  @Override
  public Vertex decodeVertex(Map<Object, Object> vertex) throws Exception {
    Object payload = NeptuneGraphSerializer.property(vertex, "payload");
    if (payload != null) {
      return JacksonMapper.mapper.readValue(payload.toString(), vertexType);
    }
    return JacksonMapper.mapper.convertValue(NeptuneGraphSerializer.properties(vertex), vertexType);
  }

  @Override
  public String edgeLabel(Edge edge) {
    return edgeLabel;
  }

  @Override
  public Identifier edgeIdentifier(Edge edge) {
    return edgeIdentifier.apply(edge);
  }

  @Override
  public Identifier edgeFrom(Edge edge) {
    return edgeFrom.apply(edge);
  }

  @Override
  public Identifier edgeTo(Edge edge) {
    return edgeTo.apply(edge);
  }

  @Override
  public Map<String, Object> encodeEdge(Edge edge) throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put("id", encodeIdentifier(edgeIdentifier(edge)));
    properties.put("fromId", encodeIdentifier(edgeFrom(edge)));
    properties.put("toId", encodeIdentifier(edgeTo(edge)));
    properties.put("payload", JacksonMapper.mapper.writeValueAsString(edge));
    edgeProperties.forEach((name, value) -> {
      Object property = value.apply(edge);
      if (property != null) {
        properties.put(name, property.toString());
      }
    });
    return Map.copyOf(properties);
  }

  @Override
  public Edge decodeEdge(Map<Object, Object> edge) throws Exception {
    Object payload = NeptuneGraphSerializer.property(edge, "payload");
    if (payload != null) {
      return JacksonMapper.mapper.readValue(payload.toString(), edgeType);
    }
    return JacksonMapper.mapper.convertValue(NeptuneGraphSerializer.properties(edge), edgeType);
  }

  @Override
  public Object encodeIdentifier(Identifier identifier) {
    return identifier == null ? null : identifierEncoder.apply(identifier);
  }
}
