package dev.haypacomer.application.quantity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.InvalidQuantityException;
import dev.haypacomer.domain.quantity.UnconvertibleQuantityException;
import dev.haypacomer.domain.quantity.Unit;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InterpretQuantityTest {

  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final InterpretQuantity interpretQuantity = new InterpretQuantity(stores.catalog);

  InterpretQuantityTest() {
    stores.catalog.save(
        new FoodMetadata(
            "Egg",
            FoodCategory.EGGS,
            Unit.PIECE,
            ConversionFactors.withPieceWeight("50"),
            true,
            21,
            Set.of()));
  }

  @Test
  void interpretsTextWithTheFoodConversionFactors() {
    QuantityInterpretation interpretation = interpretQuantity.interpret("egg", "3 huevos + 1 egg");

    assertEquals("Egg", interpretation.food().name());
    assertEquals(Grams.of(200), interpretation.grams());
    assertEquals("3 pc + 1 pc", interpretation.expression().toString());
  }

  @Test
  void reportsUnknownFoodsAndUnreadableQuantities() {
    assertThrows(
        FoodNotInCatalogException.class, () -> interpretQuantity.interpret("Unicorn", "1 g"));
    assertThrows(InvalidQuantityException.class, () -> interpretQuantity.interpret("Egg", "a few"));
    assertThrows(
        UnconvertibleQuantityException.class, () -> interpretQuantity.interpret("Egg", "1 cup"));
  }
}
