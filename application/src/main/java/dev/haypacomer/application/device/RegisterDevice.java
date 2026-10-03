package dev.haypacomer.application.device;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;

public final class RegisterDevice {

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final DeviceRepository devices;
  private final DeviceKeys keys;
  private final Clock clock;

  public RegisterDevice(
      HouseholdRepository households,
      FridgeRepository fridges,
      DeviceRepository devices,
      OpaqueTokens opaqueTokens,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.devices = Objects.requireNonNull(devices, "devices");
    this.keys = new DeviceKeys(opaqueTokens);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public RegisteredDevice register(
      UserId actor, HouseholdId householdId, FridgeId fridgeId, String name, DeviceKind kind) {
    Household household = households.get(actor, householdId);
    household.requirePermission(actor, Permission.MANAGE_DEVICES);
    boolean ownFridge =
        fridges.findByHousehold(household.id()).stream().map(Fridge::id).anyMatch(fridgeId::equals);
    if (!ownFridge) {
      throw new IllegalArgumentException("The fridge does not belong to this household");
    }
    String rawKey = keys.generate();
    Device device =
        Device.register(household.id(), fridgeId, name, kind, keys.hash(rawKey), clock.instant());
    devices.save(device);
    return new RegisteredDevice(device, rawKey);
  }
}
