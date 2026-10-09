package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.CHICKEN;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.i18n.LocaleOption;
import dev.haypacomer.application.i18n.TranslateMessages;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.identity.User;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostgresTranslationRepositoryTest extends PostgresTestSupport {

  @Test
  void seedsSpanishByDefaultAndEnglish() {
    PostgresTranslationRepository translations =
        new PostgresTranslationRepository(dataSource, Clock.systemUTC(), Duration.ofMinutes(5));

    assertEquals(
        List.of(
            new LocaleOption("es-CO", "Español", true), new LocaleOption("en", "English", false)),
        translations.locales());
    assertEquals("Leche", translations.texts("es-CO").get("food.milk"));
    assertEquals("Milk", translations.texts("en").get("food.milk"));
    assertEquals(24, translations.texts("en").size());
    assertTrue(translations.texts("es-CO").size() > 24);
    assertTrue(translations.templates("en").isEmpty());
    assertEquals(
        "La puerta de la nevera lleva 45 s abierta. Ciérrala para mantener la comida fría.",
        new TranslateMessages(translations)
            .into("es-CO")
            .translate("The fridge door has been open for 45 s. Close it to keep the food cold."));
  }

  @Test
  void savesTheLanguageOfEachUser() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    PostgresUserLocaleRepository locales = new PostgresUserLocaleRepository(dataSource);

    assertTrue(locales.locale(juan.id()).isEmpty());
    locales.save(juan.id(), "en");

    assertEquals("en", locales.locale(juan.id()).orElseThrow());
  }

  @Test
  void catalogSearchFindsFoodsByTheirSpanishName() {
    PostgresFoodCatalogRepository catalog = new PostgresFoodCatalogRepository(dataSource);
    catalog.save(MILK);
    catalog.save(CHICKEN);

    assertEquals(
        List.of("chicken breast"),
        catalog.search("pechu", 5).stream().map(FoodMetadata::key).toList());
    assertEquals(
        List.of("milk"), catalog.search("Lech", 5).stream().map(FoodMetadata::key).toList());
    assertEquals(
        List.of("milk"), catalog.search("mil", 5).stream().map(FoodMetadata::key).toList());
    assertTrue(catalog.search("zzz", 5).isEmpty());
  }
}
