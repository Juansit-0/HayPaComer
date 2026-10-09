package dev.haypacomer.application.i18n;

import dev.haypacomer.application.port.TranslationRepository;
import java.util.List;
import java.util.Objects;

public final class ListLocales {

  private final TranslationRepository translations;

  public ListLocales(TranslationRepository translations) {
    this.translations = Objects.requireNonNull(translations, "translations");
  }

  public List<LocaleOption> list() {
    return translations.locales();
  }
}
