package dev.haypacomer.application.port;

import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.util.List;

public interface SensorEventDecoder {

  List<SensorEvent> decode(Device device, String payload);
}
