package com.github.jelatinone.model.struct;

import java.util.UUID;

import lombok.NonNull;

public sealed interface Identity
    permits Identity.DomainIdentity, Identity.TargetIdentity, Identity.EntityIdentity, Identity.EdgeIdentity,
    Identity.ReviewIdentity {

  @NonNull
  UUID identifier();

  static DomainIdentity domain(@NonNull UUID identifier) {
    return new DomainIdentity(identifier);
  }

  static TargetIdentity target(@NonNull UUID identifier) {
    return new TargetIdentity(identifier);
  }

  static EntityIdentity entity(@NonNull UUID identifier) {
    return new EntityIdentity(identifier);
  }

  static EdgeIdentity edge(@NonNull UUID identifier) {
    return new EdgeIdentity(identifier);
  }

  static ReviewIdentity review(@NonNull UUID identifier) {
    return new ReviewIdentity(identifier);
  }

  public record DomainIdentity(@NonNull UUID identifier) implements Identity {
  }

  public record TargetIdentity(@NonNull UUID identifier) implements Identity {
  }

  public record EntityIdentity(@NonNull UUID identifier) implements Identity {
  }

  public record EdgeIdentity(@NonNull UUID identifier) implements Identity {
  }

  public record ReviewIdentity(@NonNull UUID identifier) implements Identity {
  }
}
