package dev.haypacomer.application.device;

public final class InvalidDeviceKeyException extends RuntimeException {

  public InvalidDeviceKeyException() {
    super("Invalid device key");
  }
}
