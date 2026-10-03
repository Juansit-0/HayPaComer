package dev.haypacomer.application.auth;

public record RegisterCommand(String email, String password, String displayName) {

  @Override
  public String toString() {
    return "RegisterCommand[email=" + email + ", displayName=" + displayName + "]";
  }
}
