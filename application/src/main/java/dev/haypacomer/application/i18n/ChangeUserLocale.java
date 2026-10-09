package dev.haypacomer.application.i18n;

import dev.haypacomer.application.port.TranslationRepository;
import dev.haypacomer.application.port.UserLocaleRepository;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public final class ChangeUserLocale {

  private final TranslationRepository translations;
  private final UserLocaleRepository users;

  public ChangeUserLocale(TranslationRepository translations, UserLocaleRepository users) {
    this.translations = Objects.requireNonNull(translations, "translations");
    this.users = Objects.requireNonNull(users, "users");
  }

  public String change(UserId actor, String locale) {
    String code =
        translations.locales().stream()
            .map(LocaleOption::code)
            .filter(candidate -> candidate.equalsIgnoreCase(locale == null ? "" : locale.strip()))
            .findFirst()
            .orElseThrow(() -> new UnknownLocaleException(locale));
    users.save(actor, code);
    return code;
  }
}
