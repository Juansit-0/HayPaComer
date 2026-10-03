package dev.haypacomer.application.audit;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditEntry(
    UserId actor,
    HouseholdId household,
    String action,
    String entity,
    UUID entityId,
    Map<String, String> detail,
    Instant at) {

  public AuditEntry {
    Objects.requireNonNull(actor, "actor");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(at, "at");
    detail = Map.copyOf(detail);
  }
}
