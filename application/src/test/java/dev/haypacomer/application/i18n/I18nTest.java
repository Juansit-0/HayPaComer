package dev.haypacomer.application.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.port.TranslationRepository;
import dev.haypacomer.application.port.UserLocaleRepository;
import dev.haypacomer.domain.identity.UserId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class I18nTest {

  private static final Map<String, String> SPANISH_TEMPLATES =
      Map.of(
          "Not found", "No encontrado",
          "Food not in catalog: {}", "El alimento no está en el catálogo: {}",
          "{} must be between {} and {}", "{} debe estar entre {} y {}",
          "{} must be a number", "{} debe ser un número",
          "Only {} min above {} C", "Solo {} min por encima de {} C");

  private final Map<UserId, String> stored = new HashMap<>();
  private final TranslationRepository translations =
      new TranslationRepository() {
        @Override
        public List<LocaleOption> locales() {
          return List.of(
              new LocaleOption("es-CO", "Español", true), new LocaleOption("en", "English", false));
        }

        @Override
        public Map<String, String> texts(String locale) {
          return locale.equals("es-CO")
              ? Map.of("food.milk", "Leche")
              : Map.of("food.milk", "Milk");
        }

        @Override
        public Map<String, String> templates(String locale) {
          return locale.equals("es-CO") ? SPANISH_TEMPLATES : Map.of();
        }
      };
  private final UserLocaleRepository users =
      new UserLocaleRepository() {
        @Override
        public Optional<String> locale(UserId user) {
          return Optional.ofNullable(stored.get(user));
        }

        @Override
        public void save(UserId user, String locale) {
          stored.put(user, locale);
        }
      };

  @Test
  void translatorMatchesExactTextAndTemplatesWithValues() {
    MessageTranslator translator = new MessageTranslator(SPANISH_TEMPLATES);

    assertEquals("No encontrado", translator.translate("Not found"));
    assertEquals(
        "El alimento no está en el catálogo: Unicorn",
        translator.translate("Food not in catalog: Unicorn"));
    assertEquals(
        "food.at-risk-days debe estar entre 0 y 14",
        translator.translate("food.at-risk-days must be between 0 and 14"));
    assertEquals("Solo 10 min por encima de 5 C", translator.translate("Only 10 min above 5 C"));
    assertEquals("Something new", translator.translate("Something new"));
    assertEquals("", translator.translate(""));
    assertEquals(null, translator.translate(null));
  }

  @Test
  void theMostSpecificTemplateWins() {
    MessageTranslator translator =
        new MessageTranslator(
            Map.of("{} must be a number", "general {}", "Price {} must be a number", "price {}"));

    assertEquals("price 7", translator.translate("Price 7 must be a number"));
    assertEquals("general Size", translator.translate("Size must be a number"));
  }

  @Test
  void resolvesUserPreferenceThenBrowserThenDefault() {
    ResolveLocale resolve = new ResolveLocale(translations, users);
    UserId ana = UserId.newId();

    assertEquals("es-CO", resolve.resolve(Optional.empty(), List.of()));
    assertEquals("en", resolve.resolve(Optional.empty(), List.of(Locale.forLanguageTag("en-US"))));
    assertEquals(
        "es-CO", resolve.resolve(Optional.empty(), List.of(Locale.forLanguageTag("es-MX"))));
    assertEquals(
        "es-CO",
        resolve.resolve(Optional.empty(), List.of(Locale.FRENCH, Locale.forLanguageTag("es-CO"))));
    assertEquals("es-CO", resolve.resolve(Optional.empty(), List.of(Locale.GERMAN)));

    new ChangeUserLocale(translations, users).change(ana, "EN");

    assertEquals("en", stored.get(ana));
    assertEquals("en", resolve.resolve(Optional.of(ana), List.of(Locale.forLanguageTag("es"))));
    assertThrows(
        UnknownLocaleException.class,
        () -> new ChangeUserLocale(translations, users).change(ana, "fr"));
    assertThrows(
        UnknownLocaleException.class,
        () -> new ChangeUserLocale(translations, users).change(ana, null));
  }

  @Test
  void bundlesAndTranslatorsComeFromTheRepository() {
    assertEquals(Map.of("food.milk", "Leche"), new ViewTranslations(translations).view("es-CO"));
    assertEquals(
        "Unknown locale fr",
        assertThrows(
                UnknownLocaleException.class, () -> new ViewTranslations(translations).view("fr"))
            .getMessage());
    assertEquals(2, new ListLocales(translations).list().size());
    TranslateMessages translate = new TranslateMessages(translations);
    MessageTranslator spanish = translate.into("es-CO");

    assertSame(spanish, translate.into("es-CO"));
    assertEquals("No encontrado", spanish.translate("Not found"));
    assertEquals("Not found", translate.into("en").translate("Not found"));
  }
}
