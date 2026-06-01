package com.github.jelatinone.model.struct;

import java.net.URL;
import java.util.UUID;

import com.github.jelatinone.utility.Canonical;

import lombok.NonNull;

public sealed interface Identity
    permits Identity.TargetIdentity, Identity.EntityIdentity, Identity.SemanticIdentity, Identity.EdgeIdentity,
    Identity.ReviewIdentity,
    Identity.PipelineIdentity, Identity.DiffIdentity {

  @NonNull
  UUID identifier();

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

  public record PipelineIdentity(@NonNull UUID identifier) implements Identity {
    public static PipelineIdentity create(@NonNull UUID identifier) {
      return new PipelineIdentity(identifier);
    }
  }

  public record SemanticIdentity(@NonNull UUID identifier) implements Identity {
    public static SemanticIdentity create(@NonNull UUID identifier) {
      return new SemanticIdentity(identifier);
    }
  }

  public record DiffIdentity(@NonNull UUID identifier) implements Identity {
    public static DiffIdentity create(@NonNull UUID identifier) {
      return new DiffIdentity(identifier);
    }
  }
}
