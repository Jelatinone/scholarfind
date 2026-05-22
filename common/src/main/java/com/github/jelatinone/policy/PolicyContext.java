package com.github.jelatinone.policy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Request;

import lombok.NonNull;

public interface PolicyContext<Requests extends Request<Requests>, Documents extends Document<Documents>> {

  long envelopeSchemaVersion();

  @NonNull
  UUID envelopeTargetId();

  @NonNull
  UUID envelopeReviewId();

  @NonNull
  Instant envelopeReviewedAt();

  @NonNull
  Optional<Documents> retrievedDocument();

  @NonNull
  Requests receivedRequest();
}
