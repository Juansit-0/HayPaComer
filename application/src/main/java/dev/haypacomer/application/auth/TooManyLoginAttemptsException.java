package dev.haypacomer.application.auth;

public final class TooManyLoginAttemptsException extends RuntimeException {

  public TooManyLoginAttemptsException() {
    super("Too many failed attempts; try again later");
  }
}
