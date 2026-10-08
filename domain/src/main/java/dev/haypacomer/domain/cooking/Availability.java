package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.util.HashMap;
import java.util.Map;

public final class Availability {

  private final Map<String, Grams> byFood = new HashMap<>();

  public Availability add(FoodMetadata food, Grams grams) {
    byFood.merge(food.key(), grams, Grams::plus);
    return this;
  }

  public Grams of(FoodMetadata food) {
    return byFood.getOrDefault(food.key(), Grams.ZERO);
  }
}
