package com.github.jelatinone.infra.construct;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.github.jelatinone.meta.construct.Router;
import com.github.jelatinone.model.audit.ExecutionStage;
import com.github.jelatinone.model.struct.Request;
import com.github.jelatinone.model.transit.Letter;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExecutionRouter implements Router {

  Map<ExecutionStage, RouteBinding<?>> bindings;

  public ExecutionRouter(@NonNull Collection<RouteBinding<?>> bindings) {
    Map<ExecutionStage, RouteBinding<?>> nextBindings = new EnumMap<>(ExecutionStage.class);
    bindings.forEach(binding -> {
      RouteBinding<?> previousBinding = nextBindings.put(binding.forwardRef(), binding);
      if (previousBinding != null) {
        throw new IllegalArgumentException(
            String.format("Duplicate emission binding configured for %s stage", binding.forwardRef()));
      }
    });
    this.bindings = Map.copyOf(nextBindings);
  }

  @SafeVarargs
  public static ExecutionRouter of(RouteBinding<?>... bindings) {
    return new ExecutionRouter(List.of(bindings));
  }

  public static <R extends Request> RouteBinding<R> bind(
      ExecutionStage forwardRef,
      Class<R> payloadType,
      Consumer<Letter<R>> forwardTo) {
    return new RouteBinding<>(forwardRef, payloadType, forwardTo);
  }

  @Override
  public void route(Letter<?> envelope) {
    RouteBinding<?> binding = bindings.get(envelope.executionRef());
    if (binding == null) {
      throw new IllegalStateException(
          String.format("No emission binding configured for %s stage", envelope.executionRef()));
    }
    binding.send(envelope);
  }

  public static record RouteBinding<R extends Request>(
      ExecutionStage forwardRef,
      Class<R> payloadType,
      Consumer<Letter<R>> forwardTo) {

    @SuppressWarnings("unchecked")
    public void send(Letter<? extends Request> envelope) {
      Request payload = envelope.content();
      if (payload == null || !payloadType.isInstance(payload)) {
        throw new IllegalArgumentException(String.format(
            "Emission payload type %s is not supported for %s stage",
            payload == null
                ? "null"
                : payload.getClass().getName(),
            forwardRef()));
      }
      forwardTo.accept((Letter<R>) envelope);
    }
  }

}
