package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.CHICKEN;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.EGG;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Unit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresFoodCatalogRepositoryTest extends PostgresTestSupport {

  private PostgresFoodCatalogRepository catalog;

  @BeforeEach
  void createRepository() {
    catalog = new PostgresFoodCatalogRepository(dataSource);
  }

  @Test
  void roundTripsMetadataWithFactorsAndAllergens() {
    catalog.save(MILK);
    catalog.save(EGG);
    catalog.save(CHICKEN);

    assertEquals(MILK, catalog.findByName(" milk ").orElseThrow());
    assertEquals(EGG, catalog.findByName("EGG").orElseThrow());
    assertEquals(CHICKEN, catalog.findByName("Chicken breast").orElseThrow());
    assertTrue(catalog.findByName("Tuna").isEmpty());
  }

  @Test
  void updatesByNameWithoutDuplicating() {
    catalog.save(MILK);
    FoodMetadata lactoseFree =
        new FoodMetadata(
            "MILK",
            FoodCategory.DAIRY,
            Unit.LITER,
            ConversionFactors.withDensity("1.03"),
            true,
            10,
            Set.of());

    catalog.save(lactoseFree);

    assertEquals(lactoseFree, catalog.findByName("milk").orElseThrow());
    assertEquals(1, catalog.search("mi", 10).size());
  }

  @Test
  void searchesByPrefixInOrder() {
    catalog.save(CHICKEN);
    catalog.save(EGG);
    catalog.save(
        new FoodMetadata(
            "Chickpeas",
            FoodCategory.LEGUME,
            Unit.GRAM,
            ConversionFactors.MASS_ONLY,
            false,
            365,
            Set.of(Allergen.SESAME)));

    List<String> names = catalog.search("chick", 10).stream().map(FoodMetadata::name).toList();

    assertEquals(List.of("Chicken breast", "Chickpeas"), names);
    assertEquals(1, catalog.search("chick", 1).size());
    assertTrue(catalog.search("%", 10).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> catalog.search("a", 0));
  }
}
