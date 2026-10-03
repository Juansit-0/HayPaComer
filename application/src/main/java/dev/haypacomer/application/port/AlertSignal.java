package dev.haypacomer.application.port;

import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.domain.device.DeviceId;
import java.util.List;

public interface AlertSignal {

  void signal(DeviceId device, AlertPattern pattern);

  List<AlertPattern> drain(DeviceId device);
}
