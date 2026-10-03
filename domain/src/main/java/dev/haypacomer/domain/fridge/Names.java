package dev.haypacomer.domain.fridge;

import java.util.Objects;

final class Names {

  private Names() {}

  static String require(String name, String what) {
    Objects.requireNonNull(name, what);
    String stripped = name.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException(what + " cannot be blank");
    }
    return stripped;
  }
}
