package com.github.jelatinone.policy;

import java.time.Instant;
import java.util.Optional;

import com.github.jelatinone.model.struct.Document;
import com.github.jelatinone.model.struct.Identity.ReviewIdentity;
import com.github.jelatinone.model.struct.Identity.TargetIdentity;
import com.github.jelatinone.model.struct.Request;

import lombok.NonNull;

public interface PolicyContext<Requests extends Request<Requests>, Documents extends Document<Documents>> {

  long envelopeSchemaVersion();

  @NonNull
  TargetIdentity envelopeTargetId();

  @NonNull
  ReviewIdentity envelopeReviewId();

  @NonNull
  Instant envelopeReviewedAt();

  @NonNull
  Optional<Documents> retrievedDocument();

  @NonNull
  Requests receivedRequest();
}
