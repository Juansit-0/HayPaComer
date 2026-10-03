package dev.haypacomer.application.sensor;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.util.List;

public final class ObserveSensorEvent {

  private final AlertDispatcher dispatcher;

  public ObserveSensorEvent(
      FridgeMonitorRegistry registry, DeviceRepository devices, HardwareFactories hardware) {
    this.dispatcher = new AlertDispatcher(registry, devices, hardware);
  }

  public List<Finding> observe(Device device, SensorEvent event) {
    if (!event.device().equals(device.id()) || !event.fridge().equals(device.fridge())) {
      throw new IllegalArgumentException("Event does not belong to this device");
    }
    FridgeMonitor monitor = dispatcher.registry().monitor(device.fridge());
    synchronized (monitor) {
      monitor.record(event);
    }
    dispatcher.registry().rememberDevice(device.fridge(), device.id());
    return dispatcher.evaluate(device.fridge(), event.occurredAt());
  }
}
