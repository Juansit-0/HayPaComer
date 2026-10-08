package dev.haypacomer.web.cooking;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.cooking.StrategyKind;
import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RequirementEvaluation;
import dev.haypacomer.domain.cooking.RequirementVerdict;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecipeController {

  private final EvaluateRecipe evaluateRecipe;
  private final FoodCatalogRepository catalog;

  public RecipeController(EvaluateRecipe evaluateRecipe, FoodCatalogRepository catalog) {
    this.evaluateRecipe = evaluateRecipe;
    this.catalog = catalog;
  }

  @PostMapping("/api/v1/households/{householdId}/recipes/evaluate")
  EvaluationResponse evaluate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody EvaluateRequest request) {
    Recipe recipe =
        new Recipe(
            RecipeId.newId(),
            request.name(),
            request.servings(),
            request.minutes(),
            RecipeSource.MANUAL,
            request.requirements().stream()
                .map(
                    requirement ->
                        new RecipeRequirement(
                            food(requirement.food()),
                            Grams.of(requirement.grams()),
                            requirement.optional()))
                .toList(),
            List.of());
    Map<String, List<String>> allowed =
        request.substitutes() == null ? Map.of() : request.substitutes();
    return EvaluationResponse.from(
        evaluateRecipe.evaluate(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            recipe,
            request.targetServings(),
            request.strategy(),
            original ->
                allowed.entrySet().stream()
                    .filter(entry -> FoodMetadata.keyOf(entry.getKey()).equals(original.key()))
                    .flatMap(entry -> entry.getValue().stream())
                    .map(this::food)
                    .toList()));
  }

  private FoodMetadata food(String name) {
    return catalog.findByName(name).orElseThrow(() -> new FoodNotInCatalogException(name));
  }

  record RequirementRequest(
      @NotBlank String food,
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal grams,
      boolean optional) {}

  record EvaluateRequest(
      @NotBlank String name,
      @Min(1) int servings,
      @Min(1) int minutes,
      @NotEmpty List<@Valid RequirementRequest> requirements,
      @Min(1) int targetServings,
      @NotNull StrategyKind strategy,
      Map<String, List<String>> substitutes) {}

  record RequirementResponse(
      String food,
      BigDecimal requiredGrams,
      BigDecimal availableGrams,
      BigDecimal shortfallGrams,
      RequirementVerdict verdict,
      String substitute) {

    static RequirementResponse from(RequirementEvaluation evaluation) {
      return new RequirementResponse(
          evaluation.requirement().food().name(),
          evaluation.requirement().grams().value(),
          evaluation.available().value(),
          evaluation.shortfall().value(),
          evaluation.verdict(),
          evaluation.substituteFood().map(FoodMetadata::name).orElse(null));
    }
  }

  record EvaluationResponse(
      RequirementVerdict verdict,
      int requestedServings,
      int achievableServings,
      List<RequirementResponse> requirements) {

    static EvaluationResponse from(RecipeEvaluation evaluation) {
      return new EvaluationResponse(
          evaluation.verdict(),
          evaluation.requestedServings(),
          evaluation.achievableServings(),
          evaluation.requirements().stream().map(RequirementResponse::from).toList());
    }
  }
}
