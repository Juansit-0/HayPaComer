package dev.haypacomer.application.i18n;

import dev.haypacomer.application.port.TranslationRepository;
import java.util.Map;
import java.util.Objects;

public final class ViewTranslations {

  private final TranslationRepository translations;

  public ViewTranslations(TranslationRepository translations) {
    this.translations = Objects.requireNonNull(translations, "translations");
  }

  public Map<String, String> view(String locale) {
    boolean known =
        translations.locales().stream().anyMatch(option -> option.code().equals(locale));
    if (!known) {
      throw new UnknownLocaleException(locale);
    }
    return translations.texts(locale);
  }
}
