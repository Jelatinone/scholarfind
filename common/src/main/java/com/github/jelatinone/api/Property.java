package com.github.jelatinone.api;

import java.time.Instant;
import java.util.UUID;

public sealed interface Property {

  record Text(String value) implements Property {
  }

  record Identifier(UUID value) implements Property {
  }

  record Number(long value) implements Property {
  }

  record Time(Instant value) implements Property {
  }

  record Boolean(boolean value) implements Property {
  }
}
