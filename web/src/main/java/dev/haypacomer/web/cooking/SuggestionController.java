package dev.haypacomer.web.cooking;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.SuggestDishes;
import dev.haypacomer.application.ai.Suggestion;
import dev.haypacomer.application.ai.SuggestionQuery;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class SuggestionController {

  private final SuggestDishes suggestDishes;
  private final RecipeAssembler assembler;

  public SuggestionController(SuggestDishes suggestDishes, RecipeAssembler assembler) {
    this.suggestDishes = suggestDishes;
    this.assembler = assembler;
  }

  @PostMapping("/suggestions")
  SuggestionsResponse suggestions(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody SuggestionsRequest request) {
    return suggest(jwt, householdId, request, false);
  }

  @PostMapping("/rescue")
  SuggestionsResponse rescue(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody SuggestionsRequest request) {
    return suggest(jwt, householdId, request, true);
  }

  private SuggestionsResponse suggest(
      Jwt jwt, UUID householdId, SuggestionsRequest request, boolean rescueOnly) {
    SuggestionQuery.Builder query =
        SuggestionQuery.builder()
            .servings(request.servings() == null ? 2 : request.servings())
            .maxMinutes(request.maxMinutes())
            .limit(request.limit() == null ? 3 : request.limit())
            .rescueOnly(rescueOnly);
    request
        .candidates()
        .forEach(
            candidate ->
                query.candidate(
                    assembler.assemble(
                        candidate.name(),
                        candidate.servings(),
                        candidate.minutes(),
                        candidate.requirements(),
                        candidate.steps())));
    if (request.diners() != null) {
      request.diners().forEach(diner -> query.diner(new MemberId(diner)));
    }
    return SuggestionsResponse.from(
        suggestDishes.suggest(CurrentUser.of(jwt), new HouseholdId(householdId), query.build()));
  }

  record CandidateRequest(
      @NotBlank String name,
      @Min(1) int servings,
      @Min(1) int minutes,
      @NotEmpty List<@Valid RequirementRequest> requirements,
      @Size(max = 50) List<@Valid StepRequest> steps) {}

  record SuggestionsRequest(
      @NotEmpty @Size(max = SuggestionQuery.MAX_CANDIDATES)
          List<@Valid CandidateRequest> candidates,
      @Min(1) Integer servings,
      @Min(1) Integer maxMinutes,
      @Min(1) @Max(3) Integer limit,
      List<UUID> diners) {}

  record SuggestionResponse(
      String recipe,
      int minutes,
      int score,
      String reason,
      List<String> rescuedFoods,
      RecipeController.EvaluationResponse evaluation) {

    static SuggestionResponse from(Suggestion suggestion) {
      return new SuggestionResponse(
          suggestion.evaluation().recipe().name(),
          suggestion.evaluation().recipe().minutes(),
          suggestion.score(),
          suggestion.reason(),
          suggestion.rescuedFoods().stream().map(FoodMetadata::name).toList(),
          RecipeController.EvaluationResponse.from(suggestion.evaluation()));
    }
  }

  record SuggestionsResponse(AdvisorSource source, List<SuggestionResponse> suggestions) {

    static SuggestionsResponse from(Suggestions suggestions) {
      return new SuggestionsResponse(
          suggestions.source(),
          suggestions.items().stream().map(SuggestionResponse::from).toList());
    }
  }
}
