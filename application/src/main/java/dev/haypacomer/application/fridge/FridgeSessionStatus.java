package dev.haypacomer.application.fridge;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record FridgeSessionStatus(FridgeId fridge, UserId user, Instant expiresAt) {

  public FridgeSessionStatus {
    Objects.requireNonNull(fridge, "fridge");
  }

  public Optional<UserId> activeUser() {
    return Optional.ofNullable(user);
  }
}
