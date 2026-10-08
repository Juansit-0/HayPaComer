package dev.haypacomer.application.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.coldchain.FridgeNotFoundException;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LiveTwinTest {

  private static final Instant NOW = Instant.parse("2026-10-08T22:00:00Z");

  private final Map<FridgeId, FridgeMonitor> monitors = new HashMap<>();
  private final FridgeMonitorRegistry registry =
      new FridgeMonitorRegistry() {
        @Override
        public FridgeMonitor monitor(FridgeId fridge) {
          return monitors.computeIfAbsent(
              fridge, id -> new FridgeMonitor(id, FridgeThresholds.DEFAULT));
        }

        @Override
        public void rememberDevice(FridgeId fridge, DeviceId device) {}

        @Override
        public Optional<DeviceId> lastDevice(FridgeId fridge) {
          return Optional.empty();
        }

        @Override
        public Collection<FridgeId> fridges() {
          return monitors.keySet();
        }

        @Override
        public boolean firstReport(FridgeId fridge, Finding finding) {
          return true;
        }
      };

  @Test
  void theTwinShowsTheLatestDoorAndTemperatureOrNothingYet() {
    InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
    InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
    UserId juan = UserId.newId();
    Household household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    households.save(household);
    Fridge fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    ViewFridgeTwin twin = new ViewFridgeTwin(households, fridges, registry);

    FridgeTwin unknown = twin.view(juan, household.id(), fridge.id());
    assertTrue(unknown.door().isEmpty());
    assertTrue(unknown.temperature().isEmpty());

    DeviceId door = DeviceId.newId();
    registry
        .monitor(fridge.id())
        .record(
            new DoorEvent(
                new SensorEventId(UUID.randomUUID()), door, fridge.id(), NOW, DoorState.OPEN));
    registry
        .monitor(fridge.id())
        .record(
            new TemperatureReading(
                new SensorEventId(UUID.randomUUID()),
                door,
                fridge.id(),
                NOW.plusSeconds(5),
                new BigDecimal("4.2")));

    FridgeTwin open = twin.view(juan, household.id(), fridge.id());
    assertTrue(open.door().orElseThrow());
    assertEquals(NOW, open.doorSince());
    assertEquals(0, new BigDecimal("4.2").compareTo(open.temperature().orElseThrow()));
    assertEquals(NOW.plusSeconds(5), open.measuredAt());
    assertThrows(
        FridgeNotFoundException.class, () -> twin.view(juan, household.id(), FridgeId.newId()));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> twin.view(UserId.newId(), household.id(), fridge.id()));
  }

  @Test
  void alertsAlsoReachTheLivePanel() {
    List<LiveUpdate> received = new ArrayList<>();
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), UserId.newId(), NOW);

    new AlertsToLive(new BroadcastLiveUpdate(List.of(received::add)))
        .onNotification(
            Notification.of(
                household.id(),
                NotificationType.DOOR_LEFT_OPEN,
                "Door left open",
                "Close it",
                NOW));
    BroadcastLiveUpdate.NOBODY.publish(
        LiveUpdate.of(household.id(), LiveUpdateKind.SENSOR, null, "ignored", NOW));

    assertEquals(1, received.size());
    assertEquals(LiveUpdateKind.ALERT, received.getFirst().kind());
    assertEquals("Door left open. Close it", received.getFirst().detail());
  }
}
