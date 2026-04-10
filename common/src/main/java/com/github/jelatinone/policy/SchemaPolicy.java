package com.github.jelatinone.policy;

import com.github.jelatinone.models.shared.StageDocument;

public final class SchemaPolicy<Document extends StageDocument<Document>, Context extends PolicyContext<Document>, State>
		implements Policy<Context, State> {
	private final long expectedVersion;

	public SchemaPolicy(long expectedVersion) {
		this.expectedVersion = expectedVersion;
	}

	@Override
	public PolicyStep<State> apply(Context context, State state) {
		if (context.document().documentHeader().schemaVersion() != expectedVersion) {
			return new PolicyStep.Decide<>(
					PolicyDecision.drop(state, PolicyReason.SCHEMA_MISMATCH, "Stage document schema version mismatch"));
		}
		return new PolicyStep.Continue<>(state);
	}
}
