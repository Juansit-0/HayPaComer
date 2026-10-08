package dev.haypacomer.application.port;

import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import java.util.List;
import java.util.Optional;

public interface RecipeTemplateRepository {

  List<Recipe> templates();

  Optional<Recipe> template(RecipeId id);
}
