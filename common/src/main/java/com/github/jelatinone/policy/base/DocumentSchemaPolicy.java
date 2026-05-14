package com.github.jelatinone.policy.base;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.policy.Policy;
import com.github.jelatinone.policy.PolicyContext;
import com.github.jelatinone.policy.PolicyDecision;
import com.github.jelatinone.policy.PolicyReason;
import com.github.jelatinone.policy.PolicyStep;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class DocumentSchemaPolicy<Documents extends Document<Documents>, Context extends PolicyContext<Documents>, State>
    implements Policy<Context, State> {

  long documentSchemaVersion;

  @Override
  public PolicyStep<State> apply(Context context, State state) {
    if (context.document().documentHeader().schemaVersion() != documentSchemaVersion) {
      return new PolicyStep.Decide<>(
          new PolicyDecision.Drop<State>(
              state,
              PolicyReason.SCHEMA_MISMATCH,
              "Stage document schema version mismatch"));
    }
    return new PolicyStep.Continue<>(state);
  }
}
