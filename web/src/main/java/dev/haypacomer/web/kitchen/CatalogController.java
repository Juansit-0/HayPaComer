package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.inventory.SearchFoods;
import dev.haypacomer.application.quantity.InterpretQuantity;
import dev.haypacomer.application.quantity.QuantityInterpretation;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Unit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/foods")
public class CatalogController {

  private final SearchFoods searchFoods;
  private final InterpretQuantity interpretQuantity;

  public CatalogController(SearchFoods searchFoods, InterpretQuantity interpretQuantity) {
    this.searchFoods = searchFoods;
    this.interpretQuantity = interpretQuantity;
  }

  @GetMapping
  List<FoodResponse> search(@RequestParam(name = "q", defaultValue = "") String query) {
    return searchFoods.search(query).stream().map(FoodResponse::from).toList();
  }

  @PostMapping("/interpret")
  InterpretationResponse interpret(@Valid @RequestBody InterpretRequest request) {
    return InterpretationResponse.from(
        interpretQuantity.interpret(request.food(), request.quantity()));
  }

  record InterpretRequest(@NotBlank String food, @NotBlank @Size(max = 120) String quantity) {}

  record InterpretationResponse(String food, String expression, BigDecimal grams) {

    static InterpretationResponse from(QuantityInterpretation interpretation) {
      return new InterpretationResponse(
          interpretation.food().name(),
          interpretation.expression().toString(),
          interpretation.grams().value());
    }
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
