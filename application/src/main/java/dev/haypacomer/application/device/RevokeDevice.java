package dev.haypacomer.application.device;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;

public final class RevokeDevice {

  private final GetHousehold households;
  private final DeviceRepository devices;
  private final Clock clock;

  public RevokeDevice(HouseholdRepository households, DeviceRepository devices, Clock clock) {
    this.households = new GetHousehold(households);
    this.devices = Objects.requireNonNull(devices, "devices");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public void revoke(UserId actor, HouseholdId householdId, DeviceId deviceId) {
    households.get(actor, householdId).requirePermission(actor, Permission.MANAGE_DEVICES);
    devices
        .findById(deviceId)
        .filter(device -> device.household().equals(householdId))
        .map(device -> device.revoke(clock.instant()))
        .ifPresentOrElse(
            devices::save,
            () -> {
              throw new DeviceNotFoundException();
            });
  }
}
