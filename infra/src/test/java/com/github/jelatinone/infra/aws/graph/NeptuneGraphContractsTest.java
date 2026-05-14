package com.github.jelatinone.infra.aws.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.Test;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query;
import com.github.jelatinone.api.graph.EdgeCriteria;
import com.github.jelatinone.api.graph.Edge;
import com.github.jelatinone.api.graph.GraphCriteria;
import com.github.jelatinone.api.graph.GraphDirection;
import com.github.jelatinone.api.graph.Vertex;
import com.github.jelatinone.infra.aws.graph.serial.JacksonNeptuneGraphSerializer;
import com.github.jelatinone.infra.aws.graph.serial.NeptuneGraphSerializer;
import com.github.jelatinone.model.Schemable;
import com.github.jelatinone.model.graph.GraphEdge;
import com.github.jelatinone.model.graph.GraphNode;

class NeptuneGraphContractsTest {

  static UUID ORIGIN = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static UUID MIDDLE = UUID.fromString("00000000-0000-0000-0000-000000000004");
  static UUID TARGET = UUID.fromString("00000000-0000-0000-0000-000000000002");
  static UUID EDGE = UUID.fromString("00000000-0000-0000-0000-000000000003");
  static UUID CHILD_EDGE = UUID.fromString("00000000-0000-0000-0000-000000000005");

  @Test
  void putVertexAndEdge_upsertsThroughGremlinTraversalApi() {
    GraphTraversalSource traversal = TinkerGraph.open().traversal();
    NeptuneGraph<TestVertex, TestEdge, UUID> graph = graph(traversal);

    graph.putVertex(new TestVertex(ORIGIN, "seed"));
    graph.putVertex(new TestVertex(TARGET, "target"));
    graph.putVertex(new TestVertex(ORIGIN, "seed-updated"));
    graph.putEdge(new TestEdge(EDGE, ORIGIN, TARGET, "parent"));

    assertEquals(2L, traversal.V().count().next());
    assertEquals("seed-updated", traversal.V().has("id", ORIGIN.toString()).values("name").next());
    assertEquals(1L, traversal.E().has("id", EDGE.toString()).count().next());
    assertEquals("parent", traversal.E().has("id", EDGE.toString()).values("kind").next());
  }

  @Test
  void vertices_queryByApiGraphCriteria_decodesTraversalResults() {
    GraphTraversalSource traversal = TinkerGraph.open().traversal();
    NeptuneGraph<TestVertex, TestEdge, UUID> graph = graph(traversal);
    graph.putVertex(new TestVertex(ORIGIN, "seed"));
    graph.putVertex(new TestVertex(TARGET, "target"));
    graph.putEdge(new TestEdge(EDGE, ORIGIN, TARGET, "parent"));

    Optional<TestVertex> vertex = graph.vertices().query(new Query.Singular<>(Criteria.identifier(TARGET)));
    Collection<TestVertex> outgoing = graph.vertices().query(new Query.Several<>(GraphCriteria.outgoing(ORIGIN), 5));

    assertEquals(new TestVertex(TARGET, "target"), vertex.orElseThrow());
    assertEquals(List.of(new TestVertex(TARGET, "target")), List.copyOf(outgoing));
    assertTrue(graph.vertices().query(new Query.Exists<>(GraphCriteria.outgoing(ORIGIN))));
  }

  @Test
  void vertices_graphCriteriaTraversesRadiusAndCanIncludeOrigin() {
    GraphTraversalSource traversal = TinkerGraph.open().traversal();
    NeptuneGraph<TestVertex, TestEdge, UUID> graph = graph(traversal);
    graph.putVertex(new TestVertex(ORIGIN, "seed"));
    graph.putVertex(new TestVertex(MIDDLE, "middle"));
    graph.putVertex(new TestVertex(TARGET, "target"));
    graph.putEdge(new TestEdge(EDGE, ORIGIN, MIDDLE, "parent"));
    graph.putEdge(new TestEdge(CHILD_EDGE, MIDDLE, TARGET, "parent"));

    Collection<TestVertex> vertices = graph.vertices().query(new Query.Several<>(
        new TestGraphCriteria(ORIGIN, 2, true, GraphDirection.OUT),
        5));

    assertEquals(
        List.of("middle", "seed", "target"),
        vertices.stream().map(TestVertex::name).sorted().collect(Collectors.toList()));
  }

  @Test
  void edges_queryByApiGraphCriteria_decodesTraversalResults() {
    GraphTraversalSource traversal = TinkerGraph.open().traversal();
    NeptuneGraph<TestVertex, TestEdge, UUID> graph = graph(traversal);
    graph.putVertex(new TestVertex(ORIGIN, "seed"));
    graph.putVertex(new TestVertex(TARGET, "target"));
    graph.putEdge(new TestEdge(EDGE, ORIGIN, TARGET, "parent"));

    Collection<TestEdge> edges = graph.edges().query(new Query.Several<>(EdgeCriteria.between(ORIGIN, TARGET), 5));

    assertEquals(List.of(new TestEdge(EDGE, ORIGIN, TARGET, "parent")), List.copyOf(edges));
    assertEquals(1L, graph.edges().query(new Query.Count<>(EdgeCriteria.to(TARGET))));
    assertEquals(1L, graph.edges().query(new Query.Count<>(EdgeCriteria.from(ORIGIN))));
  }

  @SuppressWarnings("resource")
  @Test
  void jacksonSerializer_roundTripsSchemableGraphModelKinds() {
    GraphTraversalSource traversal = TinkerGraph.open().traversal();
    NeptuneGraph<GraphNode.Target, GraphEdge.Reduce, UUID> graph = new NeptuneGraph<>(
        traversal,
        new JacksonNeptuneGraphSerializer<>(
            GraphNode.Target.class,
            GraphEdge.Reduce.class));
    GraphNode.Target target = new GraphNode.Target(ORIGIN, url("https://example.com"), java.time.Instant.EPOCH);
    GraphNode.Entity entity = new GraphNode.Entity(TARGET, UUID.randomUUID(), java.time.Instant.EPOCH);
    GraphEdge.Reduce resolvesTo = new GraphEdge.Reduce(
        EDGE,
        UUID.randomUUID(),
        target.targetId(),
        entity.entityId(),
        java.time.Instant.EPOCH);

    graph.putVertex(target);
    new NeptuneGraph<GraphNode.Entity, GraphEdge.Reduce, UUID>(
        traversal,
        new JacksonNeptuneGraphSerializer<>(
            GraphNode.Entity.class,
            GraphEdge.Reduce.class))
        .putVertex(entity);
    graph.putEdge(resolvesTo);

    Optional<GraphNode.Target> decodedTarget = graph.vertices()
        .query(new Query.Singular<>(Criteria.identifier(ORIGIN)));
    Collection<GraphEdge.Reduce> decodedEdges = graph.edges().query(new Query.Several<>(
        EdgeCriteria.between(target.targetId(), entity.entityId()),
        5));

    assertEquals(target, decodedTarget.orElseThrow());
    assertEquals(List.of(resolvesTo), List.copyOf(decodedEdges));
  }

  private static NeptuneGraph<TestVertex, TestEdge, UUID> graph(GraphTraversalSource traversal) {
    return new NeptuneGraph<>(traversal, serializer());
  }

  private static NeptuneGraphSerializer<TestVertex, TestEdge, UUID> serializer() {
    return new JacksonNeptuneGraphSerializer<>(
        TestVertex.class,
        TestEdge.class);
  }

  private static java.net.URL url(String value) {
    try {
      return java.net.URI.create(value).toURL();
    } catch (Exception exception) {
      throw new IllegalArgumentException(exception);
    }
  }

  record TestVertex(UUID id, String name, Instant emittedAt) implements Schemable, Vertex<UUID> {

    TestVertex(UUID id, String name) {
      this(id, name, Instant.EPOCH);
    }

    @Override
    public long schemaVersion() {
      return 1L;
    }

    @Override
    public UUID canonicalId() {
      return id();
    }

    @Override
    public Map<String, Object> properties() {
      return Map.of("id", id(), "name", name(), "emittedAt", emittedAt());
    }

    @Override
    public String vertexLabel() {
      return "TestVertex";
    }

    @Override
    public UUID vertexId() {
      return id();
    }
  }

  record TestEdge(UUID id, UUID from, UUID to, String kind, Instant emittedAt) implements Schemable, Edge<UUID> {

    TestEdge(UUID id, UUID from, UUID to, String kind) {
      this(id, from, to, kind, Instant.EPOCH);
    }

    @Override
    public long schemaVersion() {
      return 1L;
    }

    @Override
    public UUID canonicalId() {
      return id();
    }

    @Override
    public Map<String, Object> properties() {
      return Map.of("id", id(), "from", from(), "to", to(), "kind", kind(), "emittedAt", emittedAt());
    }

    @Override
    public String edgeLabel() {
      return "TestEdge";
    }

    @Override
    public UUID edgeId() {
      return id();
    }
  }

  record TestGraphCriteria(
      Optional<UUID> identifier,
      Optional<java.time.Duration> duration,
      int originRadius,
      boolean originIncluded,
      GraphDirection direction) implements GraphCriteria<UUID> {

    TestGraphCriteria(UUID identifier, int originRadius, boolean originIncluded, GraphDirection direction) {
      this(Optional.of(identifier), Optional.empty(), originRadius, originIncluded, direction);
    }
  }
}
