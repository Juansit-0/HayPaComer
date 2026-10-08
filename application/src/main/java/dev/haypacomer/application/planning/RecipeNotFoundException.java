package dev.haypacomer.application.planning;

public final class RecipeNotFoundException extends RuntimeException {

  public RecipeNotFoundException() {
    super("Recipe not found");
  }
}
