package dev.haypacomer.application.port;

import dev.haypacomer.domain.device.DeviceKind;

public interface HardwareFactory {

  boolean supports(DeviceKind kind);

  SensorEventDecoder decoder();

  AlertSignal alerts();
}
