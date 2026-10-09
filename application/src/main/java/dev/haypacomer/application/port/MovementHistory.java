package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.inventory.InventoryMovement;
import java.time.Instant;
import java.util.List;

public interface MovementHistory {

  List<InventoryMovement> between(HouseholdId household, Instant from, Instant to);
}
