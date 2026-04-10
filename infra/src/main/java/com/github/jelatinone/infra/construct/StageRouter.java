package com.github.jelatinone.infra.construct;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.github.jelatinone.meta.construct.Router;
import com.github.jelatinone.models.audit.ProcessingStage;
import com.github.jelatinone.models.shared.Request;
import com.github.jelatinone.models.shared.StageEnvelope;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StageRouter implements Router {

	Map<ProcessingStage, StageRouteBinding<?>> bindings;

	public StageRouter(@NonNull Collection<StageRouteBinding<?>> bindings) {
		Map<ProcessingStage, StageRouteBinding<?>> nextBindings = new EnumMap<>(ProcessingStage.class);
		bindings.forEach(binding -> {
			StageRouteBinding<?> previousBinding = nextBindings.put(binding.forwardRef(), binding);
			if (previousBinding != null) {
				throw new IllegalArgumentException(
						String.format("Duplicate emission binding configured for %s stage", binding.forwardRef()));
			}
		});
		this.bindings = Map.copyOf(nextBindings);
	}

	@SafeVarargs
	public static StageRouter of(StageRouteBinding<?>... bindings) {
		return new StageRouter(List.of(bindings));
	}

	public static <R extends Request> StageRouteBinding<R> bind(
			ProcessingStage forwardRef,
			Class<R> payloadType,
			Consumer<StageEnvelope<R>> forwardTo) {
		return new StageRouteBinding<>(forwardRef, payloadType, forwardTo);
	}

	@Override
	public void route(StageEnvelope<? extends Request> envelope) {
		StageRouteBinding<?> binding = bindings.get(envelope.stage());
		if (binding == null) {
			throw new IllegalStateException(String.format("No emission binding configured for %s stage", envelope.stage()));
		}
		binding.send(envelope);
	}

	public static record StageRouteBinding<R extends Request>(
			ProcessingStage forwardRef,
			Class<R> payloadType,
			Consumer<StageEnvelope<R>> forwardTo) {

		public void send(StageEnvelope<? extends Request> envelope) {
			Request payload = envelope.payload();
			if (payload == null || !payloadType.isInstance(payload)) {
				throw new IllegalArgumentException(String.format(
						"Emission payload type %s is not supported for %s stage",
						payload == null
								? "null"
								: payload.getClass().getName(),
						forwardRef()));
			}
			forwardTo.accept((StageEnvelope<R>) envelope);
		}
	}

}
