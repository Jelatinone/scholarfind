package com.github.jelatinone.meta;

import static org.slf4j.event.Level.*;
import static com.github.jelatinone.meta.transitory.State.*;
import static java.util.concurrent.TimeUnit.*;

import java.util.List;

import com.github.jelatinone.meta.result.CollectionResult;
import com.github.jelatinone.meta.result.PostResult;
import com.github.jelatinone.meta.transitory.State;
import com.github.jelatinone.utility.Locked;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

/**
 * 
 * <h1>SequentialTask</h1>
 * 
 * Describes a {@link Task task} that can be {@link #run() operated} in
 * sequential units of execution.
 * 
 * @author Cody Washington
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public non-sealed abstract class SequentialTask<Consumes, Produces> extends Task<Consumes, Produces> {

	@NonFinal
	Consumes operand = null;
	@NonFinal
	Produces result = null;

	/**
	 * Creates a new sequential Task
	 * 
	 * @param config Config to associate with this task
	 */
	protected SequentialTask(final @NonNull Configuration config) {
		super(config);
	}

	@Override
	public void run() {
		useMessage(String.format("Operation started : %s", _taskConfig.name), DEBUG);
		while (!_completable.isDone()) {
			try {
				State state = _state.get();
				useMessage(String.format("Operation staged : %s", state), DEBUG);
				switch (state) {

					case CREATED -> {
						setup();
						useState(COLLECTING);
					}

					case AWAITING -> {
						await();
						useMessage(String.format("Awaited jobs : %d", _failed.size()), INFO);
						useState(COLLECTING);
					}

					case COLLECTING -> {
						Locked<Consumes> failed;
						int failedCount = 0;
						while ((failed = _failed.poll()) != null) {
							_collected.offer(failed.value);
							failedCount++;
						}
						useMessage(String.format("Added failed work : %d", failedCount), DEBUG);

						CollectionResult<Consumes> result = collect();
						switch (result) {
							case CollectionResult.Alive(List<Consumes> collection) -> {
								useMessage("Collection shape : Alive", INFO);

								_collected.addAll(collection);
								_taskConfig.retryScheduler.reset();

								useMessage(String.format("Added collected work : %d", collection.size()), DEBUG);
							}

							case CollectionResult.Idle() -> useMessage("Collection shape : Idle", INFO);

							case CollectionResult.Empty() -> useMessage("Collection shape : Empty", INFO);
						}

						if (!_collected.isEmpty()) {
							setup();
							useState(OPERATING);
							return;
						}

						if (!_failed.isEmpty()) {
							useState(AWAITING);
							return;
						}

						useState(COMPLETED);
					}

					case OPERATING -> {
						operand = _collected.poll();
						if (operand == null) {
							useState(COLLECTING);
							break;
						}
						result = operate(operand);
						useState(POSTING);
					}

					case POSTING -> {
						PostResult currentStatus = post(result);

						useMessage(String.format("Posted work : %s", currentStatus), ERROR);

						switch (currentStatus) {
							case PostResult.Success ignored -> {
								_attempts.remove(operand);
								useMessage(String.format("Completed work : %s", operand), INFO);
							}

							case PostResult.Fatal ignored -> {
								_attempts.remove(operand);
								useMessage(String.format("Failed work : %s", operand), ERROR);
							}

							case PostResult.Retry ignored -> {
								int attempt = _attempts.getOrDefault(operand, 0) + 1;
								if (attempt < _taskConfig.logicalRetries) {
									long delay = _taskConfig.retryScheduler.compute(attempt);

									_attempts.put(operand, attempt);
									_failed.add(new Locked<>(operand, delay, NANOSECONDS));

									useMessage(String.format("Queued failed work : %s", operand), ERROR);
								} else {
									_attempts.remove(operand);
								}
							}
						}
					}

					case RESTARTING -> {
						restart();
						_attempts.clear();
						_failed.clear();
						useState(CREATED);
					}

					case COMPLETED, FAILED -> {
						shutdown();
						_completable.complete(null);
						return;
					}

					default -> {
						_completable.complete(null);
						throw new IllegalStateException(String.format("Accessed invalid SequentialTask state : %s", state));
					}
				}
			} catch (final Throwable throwable) {
				useState(FAILED);
				useMessage(String.format("Operation interrupted : %s", throwable.getCause()), ERROR, throwable);
				_completable.completeExceptionally(throwable);
			}
		}
		useMessage(String.format("Operation ended : %s", _taskConfig.name), DEBUG);
	}

	/**
	 * Provides the most recent consumed operand
	 * 
	 * @return Previous consumed operand
	 */
	public Consumes getConsumed() {
		return operand;
	}

	/**
	 * Provides the most recent produced operand
	 * 
	 * @return Previous produced operand
	 */
	public Produces getProduced() {
		return result;
	}
}
