package com.github.jelatinone.meta;

import static org.slf4j.event.Level.*;
import static java.util.concurrent.TimeUnit.*;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.github.jelatinone.meta.result.CollectionResult;
import com.github.jelatinone.meta.result.PostResult;
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
public non-sealed abstract class SequentialTask<Consumes, Produces>
		extends Task<SequentialTask.State, Consumes, Produces> {

	@NonFinal
	Consumes operand = null;
	@NonFinal
	Produces result = null;

	/**
	 * 
	 * <h1>State</h1>
	 * 
	 * <p>
	 * Describes the state of {@link SequentialTask#run() parallel operation} at a
	 * point during execution.
	 * </p>
	 * 
	 * @author Cody Washington
	 */
	public enum State {

		CREATED,

		AWAITING,

		COLLECTING,

		RESTARTING,

		COMPLETED,

		FAILED,

		OPERATING,

		POSTING
	}

	/**
	 * Creates a new sequential Task
	 * 
	 * @param config Config to associate with this task
	 */
	protected SequentialTask(final @NonNull Configuration config) {
		super(config);
		_state.set(State.CREATED);
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
						useState(State.COLLECTING);
					}

					case AWAITING -> {
						await();
						useMessage(String.format("Awaited jobs : %d", _failed.size()), INFO);
						useState(State.COLLECTING);
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
							case CollectionResult.Alive(Collection<Consumes> collection) -> {
								useMessage("Collection shape : Alive", INFO);

								_collected.addAll(collection);
								_taskConfig.retryScheduler.reset();

								useMessage(String.format("Added collected work : %d", collection.size()), DEBUG);
							}

							case CollectionResult.Empty() -> useMessage("Collection shape : Empty", INFO);
						}

						if (!_collected.isEmpty()) {
							setup();
							useState(State.OPERATING);
							continue;
						}

						if (!_failed.isEmpty()) {
							useState(State.AWAITING);
							continue;
						}

						useState(State.COMPLETED);
					}

					case OPERATING -> {
						operand = _collected.poll();
						if (operand == null) {
							useState(State.COLLECTING);
							break;
						}
						result = operate(operand);
						useState(State.POSTING);
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
						useState(State.OPERATING);
					}

					case RESTARTING -> {
						restart();
						_attempts.clear();
						_failed.clear();
						useState(State.CREATED);
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
				State failedFrom = _state.get();
				useState(State.FAILED);
				useMessage(String.format("Operation interrupted : %s", throwable.getCause()), ERROR, throwable);
				if (failedFrom != State.COMPLETED && failedFrom != State.FAILED) {
					try {
						shutdown();
					} catch (Throwable shutdownFailure) {
						throwable.addSuppressed(shutdownFailure);
					}
				}
				_completable.completeExceptionally(throwable);
			}
		}
		useMessage(String.format("Operation ended : %s", _taskConfig.name), DEBUG);
	}

	/**
	 * 
	 * Safely modifies the internal runtime state of this instance
	 * 
	 * @param state next state of this task instance
	 * 
	 * @apiNote Unexpected modifications to state during {@link #run() runtime} can
	 *          cause unexpected side effects
	 * 
	 */
	protected synchronized void useState(final @NonNull State state) {
		final State currentState = _state.get();
		if (currentState == State.FAILED || currentState == State.COMPLETED) {
			switch (state) {
				case FAILED, COMPLETED -> {
				}
				default -> _completable = new CompletableFuture<>();
			}
		}
		useMessage(String.format("State update : %s -> %s", currentState, state), INFO);
		this._state.set(state);
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
