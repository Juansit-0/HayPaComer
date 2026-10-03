package dev.haypacomer.domain.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeviceTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  private Device device() {
    return Device.register(
        HouseholdId.newId(),
        FridgeId.newId(),
        " Door sensor ",
        DeviceKind.ESP32_DOOR_TEMP,
        "hash",
        NOW);
  }

  @Test
  void registersActiveAndNeverSeen() {
    Device device = device();

    assertEquals("Door sensor", device.name());
    assertTrue(device.isActive());
    assertTrue(device.lastSeen().isEmpty());
    assertFalse(device.toString().contains("hash"));
  }

  @Test
  void tracksLastSeenAndRevocation() {
    Device seen = device().seenAt(NOW.plusSeconds(30));
    Device revoked = seen.revoke(NOW.plusSeconds(60));

    assertEquals(NOW.plusSeconds(30), seen.lastSeen().orElseThrow());
    assertFalse(revoked.isActive());
    assertSame(revoked, revoked.revoke(NOW.plusSeconds(90)));
  }

  @Test
  void rejectsBlankNames() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Device.register(
                HouseholdId.newId(), FridgeId.newId(), " ", DeviceKind.SIMULATOR, "h", NOW));
    assertThrows(NullPointerException.class, () -> new DeviceId(null));
  }
}
