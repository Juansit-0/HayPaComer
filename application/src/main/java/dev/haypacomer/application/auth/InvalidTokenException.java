package dev.haypacomer.application.auth;

public final class InvalidTokenException extends RuntimeException {

  public InvalidTokenException() {
    super("The link is invalid or has expired");
  }
}
