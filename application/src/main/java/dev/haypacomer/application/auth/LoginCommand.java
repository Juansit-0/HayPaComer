package dev.haypacomer.application.auth;

public record LoginCommand(String email, String password) {

  @Override
  public String toString() {
    return "LoginCommand[email=" + email + "]";
  }
}
