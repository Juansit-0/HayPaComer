package dev.haypacomer.application.device;

public final class DeviceNotFoundException extends RuntimeException {

  public DeviceNotFoundException() {
    super("Device not found");
  }
}
