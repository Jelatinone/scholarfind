package com.github.jelatinone.meta.archetype.queue;

import java.util.Collection;

import com.github.jelatinone.api.Query;
import com.github.jelatinone.api.queue.QueueCriteria;
import com.github.jelatinone.api.queue.QueueEnvelope;
import com.github.jelatinone.meta.archetype.Collect;
import com.github.jelatinone.meta.result.CollectionResult;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import com.github.jelatinone.api.queue.RetryableQueue;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueueCollect<Consumes, Queryable> implements Collect<QueueEnvelope<Consumes>> {

	Configuration<Consumes, Queryable> config;

	@Builder
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public static final class Configuration<Consumes, Queryable> {

		Query.Several<QueueCriteria<Queryable>> collectionQuery;

		RetryableQueue<Consumes, Queryable> collectionSource;
	}

	@Override
	public CollectionResult<QueueEnvelope<Consumes>> collect() {
		Collection<QueueEnvelope<Consumes>> result = config.collectionSource.query(config.collectionQuery);
		if (!result.isEmpty()) {
			return new CollectionResult.Alive<>(result);
		} else {
			return new CollectionResult.Empty<>();
		}
	}

	@Override
	public void close() throws Exception {
		config.collectionSource.close();
	}
}
