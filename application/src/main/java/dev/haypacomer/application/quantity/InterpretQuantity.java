package dev.haypacomer.application.quantity;

import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.QuantityExpression;
import dev.haypacomer.domain.quantity.QuantityParser;
import java.util.Objects;

public final class InterpretQuantity {

  private final FoodCatalogRepository catalog;

  public InterpretQuantity(FoodCatalogRepository catalog) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
  }

  public QuantityInterpretation interpret(String foodName, String text) {
    FoodMetadata food =
        catalog.findByName(foodName).orElseThrow(() -> new FoodNotInCatalogException(foodName));
    QuantityExpression expression = QuantityParser.parse(text);
    return new QuantityInterpretation(food, expression, expression.interpret(food.conversion()));
  }
}
