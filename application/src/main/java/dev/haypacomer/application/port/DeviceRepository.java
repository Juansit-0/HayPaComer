package dev.haypacomer.application.port;

import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository {

  void save(Device device);

  Optional<Device> findById(DeviceId id);

  Optional<Device> findByKeyHash(String apiKeyHash);

  List<Device> findByHousehold(HouseholdId household);
}
