package com.github.jelatinone.policy;

import com.github.jelatinone.models.shared.StageDocument;

public final class SchemaPolicy<D extends StageDocument<D>, C extends PolicyContext<D>, State>
		implements Policy<C, State> {
	private final long expectedVersion;

	public SchemaPolicy(long expectedVersion) {
		this.expectedVersion = expectedVersion;
	}

	@Override
	public PolicyStep<State> apply(C context, State state) {
		if (context.document().documentHeader().schemaVersion() != expectedVersion) {
			return new PolicyStep.Decide<>(
					PolicyDecision.drop(state, PolicyReason.SCHEMA_MISMATCH, "Stage document schema version mismatch"));
		}
		return new PolicyStep.Continue<>(state);
	}
}
