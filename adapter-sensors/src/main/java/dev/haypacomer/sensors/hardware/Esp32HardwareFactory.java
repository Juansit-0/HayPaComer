package dev.haypacomer.sensors.hardware;

import dev.haypacomer.application.port.AlertSignal;
import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.application.port.SensorEventDecoder;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.sensors.esp32.Esp32EventAdapter;

public final class Esp32HardwareFactory implements HardwareFactory {

  private final SensorEventDecoder decoder = new Esp32EventAdapter();
  private final AlertSignal alerts = new QueuedAlertSignal();

  @Override
  public boolean supports(DeviceKind kind) {
    return kind == DeviceKind.ESP32_DOOR_TEMP || kind == DeviceKind.ESP32_SCALE;
  }

  @Override
  public SensorEventDecoder decoder() {
    return decoder;
  }

  @Override
  public AlertSignal alerts() {
    return alerts;
  }
}
