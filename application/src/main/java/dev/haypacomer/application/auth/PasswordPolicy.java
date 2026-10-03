package dev.haypacomer.application.auth;

final class PasswordPolicy {

  private static final int MAX_LENGTH = 128;

  private PasswordPolicy() {}

  static void requireAcceptable(String password, AuthSettings settings) {
    if (password == null || password.isBlank()) {
      throw new InvalidPasswordException("Password is required");
    }
    if (password.length() < settings.minPasswordLength()) {
      throw new InvalidPasswordException(
          "Password needs at least " + settings.minPasswordLength() + " characters");
    }
    if (password.length() > MAX_LENGTH) {
      throw new InvalidPasswordException("Password allows at most " + MAX_LENGTH + " characters");
    }
  }
}
