package com.github.jelatinone.model.struct;

import java.net.URL;
import java.util.UUID;

import com.github.jelatinone.utility.Canonical;

import lombok.NonNull;

public sealed interface Identity
    permits Identity.DomainIdentity, Identity.TargetIdentity, Identity.EntityIdentity, Identity.EdgeIdentity,
    Identity.ReviewIdentity {

  @NonNull
  UUID identifier();

  public record DomainIdentity(@NonNull UUID identifier) implements Identity {
    public static DomainIdentity create(@NonNull UUID identifier) {
      return new DomainIdentity(identifier);
    }

    public static DomainIdentity create(@NonNull URL value) {
      URL canonical = Canonical.canonicalizeURL(value);
      return new DomainIdentity(Canonical.stableUUID(canonical.getHost()));
    }
  }

  public record TargetIdentity(@NonNull UUID identifier) implements Identity {
    public static TargetIdentity create(@NonNull UUID identifier) {
      return new TargetIdentity(identifier);
    }

    public static TargetIdentity create(@NonNull URL value) {
      URL canonical = Canonical.canonicalizeURL(value);
      return new TargetIdentity(Canonical.stableUUID(canonical.toExternalForm()));
    }
  }

  public record EntityIdentity(@NonNull UUID identifier) implements Identity {
    public static EntityIdentity create(@NonNull UUID identifier) {
      return new EntityIdentity(identifier);
    }
  }

  public record EdgeIdentity(@NonNull UUID identifier) implements Identity {
    public static EdgeIdentity create(@NonNull UUID identifier) {
      return new EdgeIdentity(identifier);
    }

    public static EdgeIdentity create(@NonNull String relation, @NonNull Identity from, @NonNull Identity to) {
      String canonical = "%s:%s:%s".formatted(relation, from.identifier(), to.identifier());
      return new EdgeIdentity(Canonical.stableUUID(canonical));
    }
  }

  public record ReviewIdentity(@NonNull UUID identifier) implements Identity {
    public static ReviewIdentity create(@NonNull UUID identifier) {
      return new ReviewIdentity(identifier);
    }
  }
}
