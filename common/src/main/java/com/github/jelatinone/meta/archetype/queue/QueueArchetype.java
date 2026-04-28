package com.github.jelatinone.meta.archetype.queue;

import com.github.jelatinone.api.Envelope;
import com.github.jelatinone.meta.transitory.Directive;

import lombok.NonNull;

public interface QueueArchetype<Consumes, Produces> {
	/**
	 * Process a consumable queue element into a produced element.
	 * 
	 * @param input Consumable element from the ordered source queue
	 * @return Produced element
	 */
	Produces elementProcess(@NonNull Consumes input);

	/**
	 * Handle an exceptional {@link #elementProcess(Object)} invocation and map it
	 * to a valid produced element that can continue through the post phase.
	 * 
	 * @param input     Consumable element from the ordered source queue
	 * @param throwable Throwable raised during {@link #elementProcess(Object)}
	 *                  processing
	 * @return Produced element
	 */
	Produces elementFailure(@NonNull Consumes input, @NonNull Throwable throwable);

	/**
	 * Resolve the directive that controls which completion hook runs and how the
	 * original queue message is acknowledged.
	 * 
	 * @param output Output of the {@link #operate(Envelope) operation} stage
	 * @return Directive controlling completion behavior for the consumed message
	 */
	Directive elementDirective(@NonNull Produces output);

	/**
	 * Handle the completion path for an element whose directive resolved to
	 * {@link Directive#COMPLETE}.
	 * 
	 * @param output Output of the {@link #operate(Envelope) operation} stage
	 * @throws Exception When an exception has occurred during completion processing
	 */
	default void onComplete(@NonNull Produces output) throws Exception {
	}

	/**
	 * Handle the retry path for an element whose directive resolved to
	 * {@link Directive#RETRY}. Implementations should explicitly publish the
	 * next retry message here when needed.
	 * 
	 * @param output Output of the {@link #operate(Envelope) operation} stage
	 * @throws Exception When an exception has occurred during retry processing
	 */
	default void onRetry(@NonNull Produces output) throws Exception {
	}

	/**
	 * Handle the terminal error path for an element whose directive resolved to
	 * {@link Directive#ERROR}. Implementations should explicitly publish any
	 * dead-letter or quarantine message here when needed.
	 * 
	 * @param output Output of the {@link #operate(Envelope) operation} stage
	 * @throws Exception When an exception has occurred during failure processing
	 */
	default void onError(@NonNull Produces output) throws Exception {
	}
}
