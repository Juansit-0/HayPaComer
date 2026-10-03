package dev.haypacomer.application.port;

import dev.haypacomer.application.audit.AuditEntry;
import dev.haypacomer.domain.household.HouseholdId;
import java.util.List;

public interface AuditLog {

  void record(AuditEntry entry);

  List<AuditEntry> recent(HouseholdId household, int limit);
}
