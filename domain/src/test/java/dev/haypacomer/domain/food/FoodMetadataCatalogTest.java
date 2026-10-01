package dev.haypacomer.domain.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Unit;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class FoodMetadataCatalogTest {

  private final FoodMetadataCatalog catalog = new FoodMetadataCatalog();

  @Test
  void sharesOneInstancePerFood() {
    FoodMetadata first = catalog.intern(FoodMetadataTest.milk("Milk"));
    FoodMetadata second = catalog.intern(FoodMetadataTest.milk("MILK"));

    assertSame(first, second);
    assertEquals(1, catalog.size());
  }

  @Test
  void findsByNameIgnoringCaseAndSpaces() {
    FoodMetadata milk = catalog.intern(FoodMetadataTest.milk("Milk"));

    assertSame(milk, catalog.find(" milk ").orElseThrow());
    assertTrue(catalog.find("Rice").isEmpty());
  }

  @Test
  void rejectsConflictingDefinitionForSameFood() {
    catalog.intern(FoodMetadataTest.milk("Milk"));
    FoodMetadata conflicting =
        new FoodMetadata(
            "Milk",
            FoodCategory.BEVERAGE,
            Unit.MILLILITER,
            ConversionFactors.withDensity("1.03"),
            true,
            7,
            Set.of());

    assertThrows(IllegalArgumentException.class, () -> catalog.intern(conflicting));
  }

  @Test
  void sharesInstanceAcrossConcurrentCallers() {
    List<FoodMetadata> interned =
        IntStream.range(0, 64)
            .parallel()
            .mapToObj(i -> catalog.intern(FoodMetadataTest.milk("Milk")))
            .toList();

    FoodMetadata canonical = interned.getFirst();
    interned.forEach(food -> assertSame(canonical, food));
    assertEquals(1, catalog.size());
  }
}
