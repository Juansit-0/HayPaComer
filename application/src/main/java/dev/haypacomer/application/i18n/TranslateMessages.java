package dev.haypacomer.application.i18n;

import dev.haypacomer.application.port.TranslationRepository;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class TranslateMessages {

  private record Compiled(Map<String, String> templates, MessageTranslator translator) {}

  private final TranslationRepository translations;
  private final Map<String, Compiled> compiled = new ConcurrentHashMap<>();

  public TranslateMessages(TranslationRepository translations) {
    this.translations = Objects.requireNonNull(translations, "translations");
  }

  public MessageTranslator into(String locale) {
    Map<String, String> templates = translations.templates(locale);
    Compiled known = compiled.get(locale);
    if (known != null && known.templates() == templates) {
      return known.translator();
    }
    MessageTranslator translator = new MessageTranslator(templates);
    compiled.put(locale, new Compiled(templates, translator));
    return translator;
  }
}
