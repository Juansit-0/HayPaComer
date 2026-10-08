package dev.haypacomer.application.planning;

import dev.haypacomer.application.port.RecipeTemplateRepository;
import dev.haypacomer.domain.recipe.Recipe;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ListRecipeTemplates {

  private final RecipeTemplateRepository templates;

  public ListRecipeTemplates(RecipeTemplateRepository templates) {
    this.templates = Objects.requireNonNull(templates, "templates");
  }

  public List<Recipe> list() {
    return templates.templates().stream().sorted(Comparator.comparing(Recipe::name)).toList();
  }
}
