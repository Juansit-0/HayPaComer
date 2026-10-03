package dev.haypacomer.application.audit;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.AuditLog;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Objects;

public final class ListActivity {

  private static final int MAX_LIMIT = 100;

  private final GetHousehold households;
  private final AuditLog audit;

  public ListActivity(HouseholdRepository households, AuditLog audit) {
    this.households = new GetHousehold(households);
    this.audit = Objects.requireNonNull(audit, "audit");
  }

  public List<AuditEntry> list(UserId actor, HouseholdId household, int limit) {
    households.get(actor, household);
    return audit.recent(household, Math.clamp(limit, 1, MAX_LIMIT));
  }
}
