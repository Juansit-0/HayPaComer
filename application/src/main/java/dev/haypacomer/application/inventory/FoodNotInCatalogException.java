package dev.haypacomer.application.inventory;

public final class FoodNotInCatalogException extends RuntimeException {

  public FoodNotInCatalogException(String name) {
    super("Food not in catalog: " + name);
  }
}
