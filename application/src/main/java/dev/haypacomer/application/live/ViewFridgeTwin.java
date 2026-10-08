package dev.haypacomer.application.live;

import dev.haypacomer.application.coldchain.FridgeNotFoundException;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import dev.haypacomer.domain.sensor.Measurement;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public final class ViewFridgeTwin {

  private static final BigDecimal OPEN = new BigDecimal("0.5");

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final FridgeMonitorRegistry monitors;

  public ViewFridgeTwin(
      HouseholdRepository households, FridgeRepository fridges, FridgeMonitorRegistry monitors) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.monitors = Objects.requireNonNull(monitors, "monitors");
  }

  public FridgeTwin view(UserId actor, HouseholdId householdId, FridgeId fridgeId) {
    households.get(actor, householdId);
    fridges.findByHousehold(householdId).stream()
        .filter(fridge -> fridge.id().equals(fridgeId))
        .findFirst()
        .orElseThrow(FridgeNotFoundException::new);
    FridgeMonitor monitor = monitors.monitor(fridgeId);
    Optional<Measurement> door;
    Optional<Measurement> temperature;
    synchronized (monitor) {
      door = monitor.latest("door");
      temperature = monitor.latest("temperature");
    }
    return new FridgeTwin(
        fridgeId,
        door.map(reading -> reading.value().compareTo(OPEN) > 0).orElse(null),
        door.map(Measurement::at).orElse(null),
        temperature.map(Measurement::value).orElse(null),
        temperature.map(Measurement::at).orElse(null));
  }
}
