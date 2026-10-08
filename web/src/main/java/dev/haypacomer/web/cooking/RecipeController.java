package dev.haypacomer.web.cooking;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.cooking.StrategyKind;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RequirementEvaluation;
import dev.haypacomer.domain.cooking.RequirementVerdict;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecipeController {

  private final EvaluateRecipe evaluateRecipe;
  private final RecipeAssembler assembler;

  public RecipeController(EvaluateRecipe evaluateRecipe, RecipeAssembler assembler) {
    this.evaluateRecipe = evaluateRecipe;
    this.assembler = assembler;
  }

  @PostMapping("/api/v1/households/{householdId}/recipes/evaluate")
  EvaluationResponse evaluate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody EvaluateRequest request) {
    Recipe recipe =
        assembler.assemble(
            request.name(),
            request.servings(),
            request.minutes(),
            request.requirements(),
            List.of());
    return EvaluationResponse.from(
        evaluateRecipe.evaluate(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            recipe,
            request.targetServings(),
            request.strategy(),
            request.diners() == null
                ? Set.of()
                : request.diners().stream().map(MemberId::new).collect(Collectors.toSet())));
  }

  record EvaluateRequest(
      @NotBlank String name,
      @Min(1) int servings,
      @Min(1) int minutes,
      @NotEmpty List<@Valid RequirementRequest> requirements,
      @Min(1) int targetServings,
      @NotNull StrategyKind strategy,
      List<UUID> diners) {}

  record RequirementResponse(
      String food,
      BigDecimal requiredGrams,
      BigDecimal availableGrams,
      BigDecimal shortfallGrams,
      RequirementVerdict verdict,
      String substitute,
      BigDecimal substituteGrams) {

    static RequirementResponse from(RequirementEvaluation evaluation) {
      return new RequirementResponse(
          evaluation.requirement().food().name(),
          evaluation.requirement().grams().value(),
          evaluation.available().value(),
          evaluation.shortfall().value(),
          evaluation.verdict(),
          evaluation.proposal().map(proposal -> proposal.substitute().name()).orElse(null),
          evaluation.proposal().map(proposal -> proposal.substituteGrams().value()).orElse(null));
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
