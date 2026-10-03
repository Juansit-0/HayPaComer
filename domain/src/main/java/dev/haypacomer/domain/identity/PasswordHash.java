package dev.haypacomer.domain.identity;

import java.util.Objects;

public record PasswordHash(String value) {

  public PasswordHash {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("Password hash cannot be blank");
    }
  }

  @Override
  public String toString() {
    return "PasswordHash[protected]";
  }
}
