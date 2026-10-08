package dev.haypacomer.application.session;

import dev.haypacomer.application.device.DeviceNotFoundException;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.session.CookingSession;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class StartCookingSession {

  private final GetHousehold households;
  private final CookingSessionRepository sessions;
  private final DeviceRepository devices;
  private final KitchenMediator mediator;
  private final Clock clock;

  public StartCookingSession(
      HouseholdRepository households,
      CookingSessionRepository sessions,
      DeviceRepository devices,
      KitchenMediator mediator,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.devices = Objects.requireNonNull(devices, "devices");
    this.mediator = Objects.requireNonNull(mediator, "mediator");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public CookingSession start(
      UserId actor,
      HouseholdId householdId,
      Recipe recipe,
      int servings,
      Optional<DeviceId> scale) {
    households.get(actor, householdId).requirePermission(actor, Permission.COOK);
    sessions
        .active(householdId)
        .ifPresent(
            active -> {
              throw new SessionAlreadyActiveException(active.id());
            });
    Instant now = clock.instant();
    CookingSession session = CookingSession.start(householdId, recipe, servings, actor, now);
    scale.map(id -> scaleOf(householdId, id)).ifPresent(session::useScale);
    sessions.save(session);
    mediator.notify(new KitchenEvent.SessionChanged(session, now));
    return session;
  }

  private DeviceId scaleOf(HouseholdId householdId, DeviceId id) {
    return devices
        .findById(id)
        .filter(device -> device.household().equals(householdId))
        .filter(device -> device.kind() != DeviceKind.ESP32_DOOR_TEMP)
        .filter(Device::isActive)
        .map(Device::id)
        .orElseThrow(DeviceNotFoundException::new);
  }
}
