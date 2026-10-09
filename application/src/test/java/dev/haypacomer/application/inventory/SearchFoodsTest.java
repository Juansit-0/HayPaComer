package dev.haypacomer.application.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Unit;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchFoodsTest {

  @Test
  void searchesByPrefixAndIgnoresBlankQueries() {
    InMemoryInventoryStores stores = new InMemoryInventoryStores();
    stores.catalog.save(
        new FoodMetadata(
            "Milk",
            FoodCategory.DAIRY,
            Unit.MILLILITER,
            ConversionFactors.MASS_ONLY,
            true,
            7,
            Set.of()));
    SearchFoods search = new SearchFoods(stores.catalog);

    assertEquals("Milk", search.search("mi").getFirst().name());
    assertEquals("Milk", search.search(" ").getFirst().name());
    assertEquals(1, search.search(null).size());
  }
}
