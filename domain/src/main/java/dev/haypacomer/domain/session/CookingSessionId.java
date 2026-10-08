package dev.haypacomer.domain.session;

import java.util.Objects;
import java.util.UUID;

public record CookingSessionId(UUID value) {

  public CookingSessionId {
    Objects.requireNonNull(value, "value");
  }

  public static CookingSessionId newId() {
    return new CookingSessionId(UUID.randomUUID());
  }
}
