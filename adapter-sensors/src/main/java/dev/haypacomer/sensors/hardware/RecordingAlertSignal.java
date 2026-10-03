package dev.haypacomer.sensors.hardware;

import dev.haypacomer.application.port.AlertSignal;
import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.domain.device.DeviceId;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

public final class RecordingAlertSignal implements AlertSignal {

  private static final Logger LOG = System.getLogger(RecordingAlertSignal.class.getName());

  private final QueuedAlertSignal recorded = new QueuedAlertSignal();

  @Override
  public void signal(DeviceId device, AlertPattern pattern) {
    LOG.log(Level.INFO, "Simulated device {0} signals {1}", device.value(), pattern);
    recorded.signal(device, pattern);
  }

  @Override
  public List<AlertPattern> drain(DeviceId device) {
    return recorded.drain(device);
  }
}
