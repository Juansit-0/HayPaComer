package dev.haypacomer.persistence.resilience;

import dev.haypacomer.application.port.RecipeTemplateRepository;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import java.util.List;
import java.util.Optional;

public final class ResilientRecipeTemplates implements RecipeTemplateRepository {

  private final RecipeTemplateRepository delegate;
  private final ResilientReads reads;

  public ResilientRecipeTemplates(RecipeTemplateRepository delegate, ResilientReads reads) {
    this.delegate = delegate;
    this.reads = reads;
  }

  @Override
  public List<Recipe> templates() {
    return reads.read("all", delegate::templates);
  }

  @Override
  public Optional<Recipe> template(RecipeId id) {
    return reads.read("id:" + id.value(), () -> delegate.template(id));
  }
}
