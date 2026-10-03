package dev.haypacomer.application.auth;

public final class EmailAlreadyRegisteredException extends RuntimeException {

  public EmailAlreadyRegisteredException() {
    super("Email is already registered");
  }
}
