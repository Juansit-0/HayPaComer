package dev.haypacomer.ai.offline;

import dev.haypacomer.application.ai.Suggestion;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.domain.cooking.FlexibleStrategy;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RequirementVerdict;
import dev.haypacomer.domain.cooking.RescueStrategy;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class RecipeRanker {

  private static final int RESCUE_POINTS = 100;
  private static final int FULL_SERVINGS_POINTS = 20;
  private static final int MINUTES_PER_PENALTY_POINT = 5;

  public List<Suggestion> rank(SuggestionRequest request) {
    return rankAll(request).stream().limit(request.limit()).toList();
  }

  public List<Suggestion> rankAll(SuggestionRequest request) {
    RescueStrategy strategy =
        new RescueStrategy(request.substitutions(), request.diners(), FlexibleStrategy.standard());
    return request.candidates().stream()
        .filter(recipe -> request.diners().canShare(recipe))
        .filter(
            recipe -> request.minutesLimit().map(limit -> recipe.minutes() <= limit).orElse(true))
        .map(
            recipe ->
                suggestion(
                    strategy.evaluate(recipe, request.availability(), request.servings()), request))
        .filter(suggestion -> suggestion.evaluation().cookable())
        .sorted(
            Comparator.comparingInt(Suggestion::score)
                .reversed()
                .thenComparing(suggestion -> suggestion.evaluation().recipe().name()))
        .toList();
  }

  private static Suggestion suggestion(RecipeEvaluation evaluation, SuggestionRequest request) {
    Recipe recipe = evaluation.recipe();
    List<FoodMetadata> rescued =
        recipe.requirements().stream()
            .map(RecipeRequirement::food)
            .filter(food -> request.atRiskFoods().contains(food.key()))
            .filter(food -> !request.availability().of(food).isZero())
            .toList();
    int score =
        rescued.size() * RESCUE_POINTS
            + verdictPoints(evaluation.verdict())
            + (evaluation.achievableServings() == evaluation.requestedServings()
                ? FULL_SERVINGS_POINTS
                : 0)
            - recipe.minutes() / MINUTES_PER_PENALTY_POINT;
    return new Suggestion(evaluation, rescued, score, reason(evaluation, rescued));
  }

  private static int verdictPoints(RequirementVerdict verdict) {
    return switch (verdict) {
      case ENOUGH -> 30;
      case SUBSTITUTE -> 15;
      case REDUCE -> 10;
      case MISSING -> 0;
    };
  }

  private static String reason(RecipeEvaluation evaluation, List<FoodMetadata> rescued) {
    StringBuilder reason = new StringBuilder();
    if (!rescued.isEmpty()) {
      reason
          .append("Uses ")
          .append(rescued.stream().map(FoodMetadata::name).collect(Collectors.joining(", ")))
          .append(" before it expires. ");
    }
    reason.append(
        switch (evaluation.verdict()) {
          case ENOUGH -> "Everything is at home for " + evaluation.requestedServings() + ".";
          case REDUCE ->
              "Enough for "
                  + evaluation.achievableServings()
                  + " of "
                  + evaluation.requestedServings()
                  + " servings.";
          case SUBSTITUTE -> "Works with an allowed substitute.";
          case MISSING -> "Something is missing.";
        });
    return reason.toString();
  }
}
