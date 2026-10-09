package dev.haypacomer.application.i18n;

import java.util.Objects;

public record LocaleOption(String code, String name, boolean isDefault) {

  public LocaleOption {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(name, "name");
  }
}
