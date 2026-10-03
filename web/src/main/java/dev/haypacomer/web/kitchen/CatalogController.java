package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.inventory.SearchFoods;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Unit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/foods")
public class CatalogController {

  private final SearchFoods searchFoods;

  public CatalogController(SearchFoods searchFoods) {
    this.searchFoods = searchFoods;
  }

  @GetMapping
  List<FoodResponse> search(@RequestParam(name = "q", defaultValue = "") String query) {
    return searchFoods.search(query).stream().map(FoodResponse::from).toList();
  }

  record FoodResponse(
      String name,
      FoodCategory category,
      Unit defaultUnit,
      BigDecimal gramsPerMilliliter,
      BigDecimal gramsPerPiece,
      boolean perishable,
      int shelfDays,
      Set<Allergen> allergens) {

    static FoodResponse from(FoodMetadata food) {
      return new FoodResponse(
          food.name(),
          food.category(),
          food.defaultUnit(),
          food.conversion().gramsPerMilliliter(),
          food.conversion().gramsPerPiece(),
          food.perishable(),
          food.shelfDays(),
          food.allergens());
    }
  }
}
