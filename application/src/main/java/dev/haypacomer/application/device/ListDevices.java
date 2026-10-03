package dev.haypacomer.application.device;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Objects;

public final class ListDevices {

  private final GetHousehold households;
  private final DeviceRepository devices;

  public ListDevices(HouseholdRepository households, DeviceRepository devices) {
    this.households = new GetHousehold(households);
    this.devices = Objects.requireNonNull(devices, "devices");
  }

  public List<Device> list(UserId actor, HouseholdId householdId) {
    households.get(actor, householdId);
    return devices.findByHousehold(householdId);
  }
}
