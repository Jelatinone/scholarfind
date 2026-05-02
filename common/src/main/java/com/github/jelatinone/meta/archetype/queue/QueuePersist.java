package com.github.jelatinone.meta.archetype.queue;

import com.github.jelatinone.api.Envelope;
import com.github.jelatinone.meta.archetype.Persist;
import com.github.jelatinone.meta.result.PostResult;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QueuePersist<T> implements Persist<Envelope<T>> {

	QueueDisposition<T> disposition;

	@Override
	public PostResult post(Envelope<T> operand) {
		if (operand == null) {
			return new PostResult.Retry(new NullPointerException("Missing envelope"));
		}

		T output = operand.content();
		try {
			switch (disposition.directive(output)) {
				case COMPLETE ->
					disposition.complete(output);
				case RETRY ->
					disposition.retry(output);
				case ERROR ->
					disposition.error(output);
			}
			operand.acknowledgement().success();
			return new PostResult.Success();
		} catch (Exception exception) {
			return new PostResult.Fatal(exception);
		}
	}
}
