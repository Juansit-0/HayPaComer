package dev.haypacomer.application.auth;

public final class InvalidPasswordException extends RuntimeException {

  public InvalidPasswordException(String message) {
    super(message);
  }
}
