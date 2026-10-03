package dev.haypacomer.sensors.hardware;

import dev.haypacomer.application.port.AlertSignal;
import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.application.port.SensorEventDecoder;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.sensors.esp32.Esp32EventAdapter;
import java.time.Clock;

public final class SimulatedHardwareFactory implements HardwareFactory {

  private final SensorEventDecoder decoder;
  private final AlertSignal alerts = new RecordingAlertSignal();

  public SimulatedHardwareFactory(Clock clock) {
    this.decoder = new SimulatedEventDecoder(new Esp32EventAdapter(), clock);
  }

  @Override
  public boolean supports(DeviceKind kind) {
    return kind == DeviceKind.SIMULATOR;
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
