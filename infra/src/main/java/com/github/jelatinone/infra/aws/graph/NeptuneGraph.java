package com.github.jelatinone.infra.aws.graph;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.github.jelatinone.api.Criteria;
import com.github.jelatinone.api.Query.Count;
import com.github.jelatinone.api.Query.Exists;
import com.github.jelatinone.api.Query.Several;
import com.github.jelatinone.api.Query.Singular;
import com.github.jelatinone.api.graph.EdgeCriteria;
import com.github.jelatinone.api.graph.Graph;
import com.github.jelatinone.api.graph.GraphCriteria;
import com.github.jelatinone.api.graph.GraphEdges;
import com.github.jelatinone.api.graph.GraphException;
import com.github.jelatinone.api.graph.GraphVertices;
import com.github.jelatinone.infra.aws.graph.serial.NeptuneGraphSerializer;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NeptuneGraph<Node, Relationship, Identifier> implements Graph<Node, Relationship, Identifier> {

  static final String ID_PROPERTY = "id";

  GraphTraversalSource graph;
  NeptuneGraphSerializer<Node, Relationship, Identifier> serializer;
  GraphVertices<Node, Identifier> vertices;
  GraphEdges<Relationship, Identifier> edges;

  public NeptuneGraph(
      Cluster cluster,
      NeptuneGraphSerializer<Node, Relationship, Identifier> serializer) {
    this(traversal().withRemote(DriverRemoteConnection.using(cluster)), serializer);
  }

  public NeptuneGraph(
      GraphTraversalSource graph,
      NeptuneGraphSerializer<Node, Relationship, Identifier> serializer) {
    this.graph = graph;
    this.serializer = serializer;
    this.vertices = new NeptuneVertices();
    this.edges = new NeptuneEdges();
  }

  @Override
  public void putVertex(Node node) {
    Object identifier;
    String label;
    Map<String, Object> properties;
    try {
      identifier = serializer.encodeIdentifier(serializer.vertexIdentifier(node));
      label = serializer.vertexLabel(node);
      properties = serializer.encodeVertex(node);
    } catch (Exception exception) {
      throw new GraphException.FatalGraphException("Failed to encode graph vertex", exception);
    }
    try {
      GraphTraversal<Vertex, Vertex> traversal = graph.V().has(ID_PROPERTY, identifier);
      if (!traversal.hasNext()) {
        traversal = graph.addV(label)
            .property(ID_PROPERTY, identifier);
      } else {
        traversal = graph.V().has(ID_PROPERTY, identifier);
      }
      properties(traversal, properties).iterate();
    } catch (GraphException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new GraphException.RetryGraphException("Failed to persist graph vertex", exception);
    }
  }

  @Override
  public void putEdge(Relationship relationship) {
    Object identifier;
    Object from;
    Object to;
    String label;
    Map<String, Object> properties;
    try {
      identifier = serializer.encodeIdentifier(serializer.edgeIdentifier(relationship));
      from = serializer.encodeIdentifier(serializer.edgeFrom(relationship));
      to = serializer.encodeIdentifier(serializer.edgeTo(relationship));
      label = serializer.edgeLabel(relationship);
      properties = serializer.encodeEdge(relationship);
    } catch (Exception exception) {
      throw new GraphException.FatalGraphException("Failed to encode graph edge", exception);
    }
    try {
      GraphTraversal<Edge, Edge> traversal = graph.E().has(ID_PROPERTY, identifier);
      GraphTraversal<?, Edge> mutation;
      if (traversal.hasNext()) {
        mutation = graph.E().has(ID_PROPERTY, identifier);
      } else {
        mutation = graph.V().has(ID_PROPERTY, from)
            .as("from")
            .V()
            .has(ID_PROPERTY, to)
            .addE(label)
            .from("from")
            .property(ID_PROPERTY, identifier);
      }
      properties(mutation, properties).iterate();
    } catch (GraphException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new GraphException.RetryGraphException("Failed to persist graph edge", exception);
    }
  }

  @Override
  public GraphEdges<Relationship, Identifier> edges() {
    return edges;
  }

  @Override
  public GraphVertices<Node, Identifier> vertices() {
    return vertices;
  }

  @Override
  public void close() throws Exception {
    graph.close();
  }

  private final class NeptuneVertices implements GraphVertices<Node, Identifier> {

    @Override
    public boolean query(Exists<Criteria<Identifier>> query) {
      return query(new Count<>(query.criteria())) > 0;
    }

    @Override
    public long query(Count<Criteria<Identifier>> query) {
      try {
        return vertex(query.criteria()).traversal(graph).count().next();
      } catch (Exception exception) {
        throw new GraphException.RetryGraphException(exception.getMessage(), exception);
      }
    }

    @Override
    public Optional<Node> query(Singular<Criteria<Identifier>> query) {
      try {
        return vertex(query.criteria()).traversal(graph)
            .elementMap()
            .tryNext()
            .map(NeptuneGraph.this::decodeVertex);
      } catch (GraphException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new GraphException.RetryGraphException(exception.getMessage(), exception);
      }
    }

    @Override
    public Collection<Node> query(Several<Criteria<Identifier>> query) {
      try {
        return vertex(query.criteria()).traversal(graph)
            .limit(query.limit())
            .elementMap()
            .toList()
            .stream()
            .map(NeptuneGraph.this::decodeVertex)
            .toList();
      } catch (GraphException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new GraphException.RetryGraphException(exception.getMessage(), exception);
      }
    }
  }

  private final class NeptuneEdges implements GraphEdges<Relationship, Identifier> {

    @Override
    public boolean query(Exists<EdgeCriteria<Identifier>> query) {
      return query(new Count<>(query.criteria())) > 0;
    }

    @Override
    public long query(Count<EdgeCriteria<Identifier>> query) {
      try {
        return edge(query.criteria()).traversal(graph).count().next();
      } catch (Exception exception) {
        throw new GraphException.RetryGraphException(exception.getMessage(), exception);
      }
    }

    @Override
    public Optional<Relationship> query(Singular<EdgeCriteria<Identifier>> query) {
      try {
        return edge(query.criteria()).traversal(graph)
            .elementMap()
            .tryNext()
            .map(NeptuneGraph.this::decodeEdge);
      } catch (GraphException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new GraphException.RetryGraphException(exception.getMessage(), exception);
      }
    }

    @Override
    public Collection<Relationship> query(Several<EdgeCriteria<Identifier>> query) {
      try {
        return edge(query.criteria()).traversal(graph)
            .limit(query.limit())
            .elementMap()
            .toList()
            .stream()
            .map(NeptuneGraph.this::decodeEdge)
            .toList();
      } catch (GraphException exception) {
        throw exception;
      } catch (Exception exception) {
        throw new GraphException.RetryGraphException(exception.getMessage(), exception);
      }
    }
  }

  private NeptuneVertexCriteria<Identifier> vertex(Criteria<Identifier> criteria) {
    return new NeptuneVertexCriteria<>(criteria, serializer::encodeIdentifier);
  }

  private NeptuneEdgeCriteria<Identifier> edge(EdgeCriteria<Identifier> criteria) {
    return new NeptuneEdgeCriteria<>(criteria, serializer::encodeIdentifier);
  }

  private Node decodeVertex(Map<Object, Object> vertex) {
    try {
      return serializer.decodeVertex(vertex);
    } catch (Exception exception) {
      throw new GraphException.FatalGraphException("Failed to decode graph vertex", exception);
    }
  }

  private Relationship decodeEdge(Map<Object, Object> edge) {
    try {
      return serializer.decodeEdge(edge);
    } catch (Exception exception) {
      throw new GraphException.FatalGraphException("Failed to decode graph edge", exception);
    }
  }

  private <Start, El extends Element> GraphTraversal<Start, El> properties(
      GraphTraversal<Start, El> traversal,
      Map<String, Object> properties) {
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      Object value = entry.getValue();
      if (value != null) {
        traversal = traversal.property(entry.getKey(), value.toString());
      }
    }
    return traversal;
  }

  private record NeptuneVertexCriteria<Identifier>(
      Criteria<Identifier> criteria,
      Function<Identifier, Object> identifierEncoder) {

    GraphTraversal<?, Vertex> traversal(GraphTraversalSource graph) {
      GraphTraversal<?, Vertex> traversal = base(graph);
      return traversal.dedup();
    }

    private GraphTraversal<?, Vertex> base(GraphTraversalSource graph) {
      if (criteria instanceof GraphCriteria<?> graphCriteria && criteria.identifier().isPresent()) {
        return origin(graph, graphCriteria);
      }
      return criteria.identifier()
          .map(identifier -> graph.V().has(ID_PROPERTY, identifierEncoder.apply(identifier)))
          .orElseGet(graph::V);
    }

    @SuppressWarnings("unchecked")
    private GraphTraversal<?, Vertex> origin(GraphTraversalSource graph, GraphCriteria<?> graphCriteria) {
      Object origin = identifierEncoder.apply(criteria.identifier().orElseThrow());
      if (graphCriteria.originIncluded()) {
        return switch (graphCriteria.direction()) {
          case OUT -> graph.V().has(ID_PROPERTY, origin)
              .union(__.identity(), __.repeat(__.out()).emit().times(graphCriteria.originRadius()));
          case IN -> graph.V().has(ID_PROPERTY, origin)
              .union(__.identity(), __.repeat(__.in()).emit().times(graphCriteria.originRadius()));
          case ANY -> graph.V().has(ID_PROPERTY, origin)
              .union(__.identity(), __.repeat(__.both()).emit().times(graphCriteria.originRadius()));
        };
      }
      return switch (graphCriteria.direction()) {
        case OUT -> graph.V().has(ID_PROPERTY, origin)
            .repeat(__.out()).emit().times(graphCriteria.originRadius());
        case IN -> graph.V().has(ID_PROPERTY, origin)
            .repeat(__.in()).emit().times(graphCriteria.originRadius());
        case ANY -> graph.V().has(ID_PROPERTY, origin)
            .repeat(__.both()).emit().times(graphCriteria.originRadius());
      };
    }
  }

  private record NeptuneEdgeCriteria<Identifier>(
      EdgeCriteria<Identifier> criteria,
      Function<Identifier, Object> identifierEncoder) {

    GraphTraversal<?, Edge> traversal(GraphTraversalSource graph) {
      GraphTraversal<?, Edge> traversal = base(graph);
      if (criteria.identifier().isPresent()) {
        traversal = traversal.has(ID_PROPERTY, identifierEncoder.apply(criteria.identifier().orElseThrow()));
      }
      return traversal.dedup();
    }

    private GraphTraversal<?, Edge> base(GraphTraversalSource graph) {
      Optional<Identifier> from = criteria.from();
      Optional<Identifier> to = criteria.to();
      if (from.isPresent() && to.isPresent()) {
        return between(graph, from.orElseThrow(), to.orElseThrow());
      }
      if (from.isPresent()) {
        return incident(graph, from.orElseThrow());
      }
      if (to.isPresent()) {
        return incident(graph, to.orElseThrow());
      }
      return graph.E();
    }

    private GraphTraversal<?, Edge> between(GraphTraversalSource graph, Identifier from, Identifier to) {
      Object fromId = identifierEncoder.apply(from);
      Object toId = identifierEncoder.apply(to);
      return switch (criteria.direction()) {
        case OUT -> graph.V().has(ID_PROPERTY, fromId)
            .outE()
            .where(__.inV().has(ID_PROPERTY, toId));
        case IN -> graph.V().has(ID_PROPERTY, fromId)
            .inE()
            .where(__.outV().has(ID_PROPERTY, toId));
        case ANY -> graph.V().has(ID_PROPERTY, fromId)
            .bothE()
            .where(__.bothV().has(ID_PROPERTY, toId));
      };
    }

    private GraphTraversal<?, Edge> incident(GraphTraversalSource graph, Identifier identifier) {
      Object encoded = identifierEncoder.apply(identifier);
      return switch (criteria.direction()) {
        case OUT -> graph.V().has(ID_PROPERTY, encoded).outE();
        case IN -> graph.V().has(ID_PROPERTY, encoded).inE();
        case ANY -> graph.V().has(ID_PROPERTY, encoded).bothE();
      };
    }
  }
}
