package com.github.jelatinone.api.queue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.jelatinone.api.Index;
import com.github.jelatinone.api.Property;

public record QueueCriteria<Identifier>(
		Optional<Identifier> id,

		Optional<Duration> queryDuration,
		Map<Index<?>, Property> queryProperties) {
	public static <Identifier> QueueCriteria<Identifier> id(Identifier id) {
		return new QueueCriteria<>(
				Optional.of(id),
				Optional.of(Duration.ofMillis(0L)),
				Map.of());
	}

	public static <Identifier> QueueCriteria<Identifier> duration(Duration duration) {
		return new QueueCriteria<>(
				Optional.empty(),
				Optional.of(duration),
				Map.of());
	}

	public static <Identifier> QueueCriteria<Identifier> property(Index<?> key, Property value) {
		return new QueueCriteria<>(
				Optional.empty(),
				Optional.of(Duration.ofMillis(0L)),
				Map.of(key, value));
	}

	public QueueCriteria<Identifier> withProperty(Index<?> key, Property value) {
		Map<Index<?>, Property> next = new HashMap<>(queryProperties) {
			{
				put(key, value);
			}
		};

		return new QueueCriteria<>(
				id,
				queryDuration,
				Map.copyOf(next));
	}
}
