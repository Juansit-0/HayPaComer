package dev.haypacomer.application.device;

import dev.haypacomer.domain.device.Device;
import java.util.Objects;

public record RegisteredDevice(Device device, String apiKey) {

  public RegisteredDevice {
    Objects.requireNonNull(device, "device");
    Objects.requireNonNull(apiKey, "apiKey");
  }

  @Override
  public String toString() {
    return "RegisteredDevice[device=" + device + "]";
  }
}
