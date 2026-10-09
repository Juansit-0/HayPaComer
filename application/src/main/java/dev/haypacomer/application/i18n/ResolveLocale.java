package dev.haypacomer.application.i18n;

import dev.haypacomer.application.port.TranslationRepository;
import dev.haypacomer.application.port.UserLocaleRepository;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ResolveLocale {

  private final TranslationRepository translations;
  private final UserLocaleRepository users;

  public ResolveLocale(TranslationRepository translations, UserLocaleRepository users) {
    this.translations = Objects.requireNonNull(translations, "translations");
    this.users = Objects.requireNonNull(users, "users");
  }

  public String resolve(Optional<UserId> user, List<Locale> accepted) {
    List<LocaleOption> options = translations.locales();
    Optional<String> chosen = user.flatMap(users::locale);
    if (chosen.isPresent()) {
      return chosen.get();
    }
    for (Locale wanted : accepted) {
      Optional<String> exact =
          options.stream()
              .map(LocaleOption::code)
              .filter(code -> code.equalsIgnoreCase(wanted.toLanguageTag()))
              .findFirst();
      if (exact.isPresent()) {
        return exact.get();
      }
      Optional<String> language =
          options.stream()
              .map(LocaleOption::code)
              .filter(
                  code -> Locale.forLanguageTag(code).getLanguage().equals(wanted.getLanguage()))
              .findFirst();
      if (language.isPresent()) {
        return language.get();
      }
    }
    return options.stream()
        .filter(LocaleOption::isDefault)
        .map(LocaleOption::code)
        .findFirst()
        .orElse("en");
  }
}
