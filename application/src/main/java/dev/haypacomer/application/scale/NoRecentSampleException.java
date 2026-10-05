package dev.haypacomer.application.scale;

public final class NoRecentSampleException extends RuntimeException {

  public NoRecentSampleException() {
    super("The scale has not reported a recent reading");
  }
}
