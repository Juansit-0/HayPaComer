package dev.haypacomer.application.sensor;

public final class MalformedSensorPayloadException extends RuntimeException {

  public MalformedSensorPayloadException(String message) {
    super(message);
  }
}
