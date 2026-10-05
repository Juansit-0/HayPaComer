package dev.haypacomer.application.scale;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.ScaleMode;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class SetScaleMode {

  private final ScaleAccess access;
  private final ScaleSessionStore sessions;

  public SetScaleMode(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleSessionStore sessions,
      Clock clock) {
    this.access = new ScaleAccess(households, devices, samples, clock);
    this.sessions = Objects.requireNonNull(sessions, "sessions");
  }

  public ScaleMode set(
      UserId actor,
      HouseholdId household,
      DeviceId deviceId,
      ScaleMode mode,
      Optional<WeighingTarget> target) {
    Device scale = access.scale(actor, household, deviceId, Permission.COOK);
    if (mode == ScaleMode.COOKING) {
      sessions.cook(
          scale.id(),
          target.orElseThrow(
              () -> new IllegalArgumentException("Cooking mode needs a food and target grams")));
    } else {
      sessions.fridge(scale.id());
    }
    return sessions.mode(scale.id());
  }
}
