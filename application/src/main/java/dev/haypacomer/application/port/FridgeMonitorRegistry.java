package dev.haypacomer.application.port;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import java.util.Collection;
import java.util.Optional;

public interface FridgeMonitorRegistry {

  FridgeMonitor monitor(FridgeId fridge);

  void rememberDevice(FridgeId fridge, DeviceId device);

  Optional<DeviceId> lastDevice(FridgeId fridge);

  Collection<FridgeId> fridges();

  boolean firstReport(FridgeId fridge, Finding finding);
}
