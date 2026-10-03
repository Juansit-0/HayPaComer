package dev.haypacomer.domain.device;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record Device(
    DeviceId id,
    HouseholdId household,
    FridgeId fridge,
    String name,
    DeviceKind kind,
    String apiKeyHash,
    Instant createdAt,
    Instant lastSeenAt,
    Instant revokedAt) {

  public Device {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(fridge, "fridge");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(apiKeyHash, "apiKeyHash");
    Objects.requireNonNull(createdAt, "createdAt");
    name = name.strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Device name cannot be blank");
    }
  }

  public static Device register(
      HouseholdId household,
      FridgeId fridge,
      String name,
      DeviceKind kind,
      String apiKeyHash,
      Instant now) {
    return new Device(DeviceId.newId(), household, fridge, name, kind, apiKeyHash, now, null, null);
  }

  public boolean isActive() {
    return revokedAt == null;
  }

  public Optional<Instant> lastSeen() {
    return Optional.ofNullable(lastSeenAt);
  }

  public Device seenAt(Instant at) {
    return new Device(id, household, fridge, name, kind, apiKeyHash, createdAt, at, revokedAt);
  }

  public Device revoke(Instant at) {
    return isActive()
        ? new Device(id, household, fridge, name, kind, apiKeyHash, createdAt, lastSeenAt, at)
        : this;
  }

  @Override
  public String toString() {
    return "Device[id=" + id + ", name=" + name + ", kind=" + kind + "]";
  }
}
