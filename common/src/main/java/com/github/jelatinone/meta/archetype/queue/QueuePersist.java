package com.github.jelatinone.meta.archetype.queue;

import com.github.jelatinone.api.Envelope;
import com.github.jelatinone.meta.archetype.Persist;
import com.github.jelatinone.meta.result.OperationResult;
import com.github.jelatinone.meta.result.PostResult;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueuePersist<T> implements Persist<Envelope<T>> {

	QueueDisposition<T> disposition;

	@Override
	public PostResult post(OperationResult<Envelope<T>> operand) {
		if (operand == null || operand.value() == null || operand.value().content() == null) {
			return PostResult.FAILURE_FATAL;
		}

		Envelope<T> envelope = operand.value();
		T output = envelope.content();
		try {
			switch (disposition.directive(output)) {
				case COMPLETE ->
					disposition.complete(output);
				case RETRY ->
					disposition.retry(output);
				case ERROR ->
					disposition.error(output);
			}
			envelope.acknowledgement().success();
			return PostResult.SUCCESS;
		} catch (Exception exception) {
			return PostResult.FAILURE_RETRY;
		}
	}
}
