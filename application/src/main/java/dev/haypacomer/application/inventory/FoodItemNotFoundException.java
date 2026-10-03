package dev.haypacomer.application.inventory;

public final class FoodItemNotFoundException extends RuntimeException {

  public FoodItemNotFoundException() {
    super("Food item not found");
  }
}
