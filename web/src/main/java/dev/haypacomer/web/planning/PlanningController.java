package dev.haypacomer.web.planning;

import dev.haypacomer.application.planning.ChangePlanEntry;
import dev.haypacomer.application.planning.CloneRecipe;
import dev.haypacomer.application.planning.CloneWeeklyPlan;
import dev.haypacomer.application.planning.GenerateWeeklyPlan;
import dev.haypacomer.application.planning.ListRecipes;
import dev.haypacomer.application.planning.SaveRecipe;
import dev.haypacomer.application.planning.ViewCurrentPlan;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.planning.Meal;
import dev.haypacomer.domain.planning.PlanEntry;
import dev.haypacomer.domain.planning.PlanEntryId;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.planning.WeeklyPlanId;
import dev.haypacomer.domain.recipe.ClonedRecipe;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.web.cooking.RecipeAssembler;
import dev.haypacomer.web.cooking.RequirementRequest;
import dev.haypacomer.web.cooking.StepRequest;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class PlanningController {

  private final SaveRecipe saveRecipe;
  private final ListRecipes listRecipes;
  private final GenerateWeeklyPlan generateWeeklyPlan;
  private final ViewCurrentPlan viewCurrentPlan;
  private final ChangePlanEntry changePlanEntry;
  private final CloneRecipe cloneRecipe;
  private final CloneWeeklyPlan cloneWeeklyPlan;
  private final RecipeAssembler assembler;

  public PlanningController(
      SaveRecipe saveRecipe,
      ListRecipes listRecipes,
      GenerateWeeklyPlan generateWeeklyPlan,
      ViewCurrentPlan viewCurrentPlan,
      ChangePlanEntry changePlanEntry,
      CloneRecipe cloneRecipe,
      CloneWeeklyPlan cloneWeeklyPlan,
      RecipeAssembler assembler) {
    this.saveRecipe = saveRecipe;
    this.listRecipes = listRecipes;
    this.generateWeeklyPlan = generateWeeklyPlan;
    this.viewCurrentPlan = viewCurrentPlan;
    this.changePlanEntry = changePlanEntry;
    this.cloneRecipe = cloneRecipe;
    this.cloneWeeklyPlan = cloneWeeklyPlan;
    this.assembler = assembler;
  }

  @PostMapping("/recipes")
  @ResponseStatus(HttpStatus.CREATED)
  RecipeResponse saveRecipe(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody RecipeRequest request) {
    return RecipeResponse.from(
        saveRecipe.save(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            assembler.assemble(
                request.name(),
                request.servings(),
                request.minutes(),
                request.requirements(),
                request.steps())));
  }

  @GetMapping("/recipes")
  List<RecipeResponse> recipes(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return listRecipes.list(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(RecipeResponse::from)
        .toList();
  }

  @PostMapping("/recipes/{recipeId}/clone")
  @ResponseStatus(HttpStatus.CREATED)
  RecipeResponse cloneRecipe(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID recipeId) {
    ClonedRecipe copy =
        cloneRecipe.clone(
            CurrentUser.of(jwt), new HouseholdId(householdId), new RecipeId(recipeId));
    return RecipeResponse.from(copy.recipe(), copy.clonedFrom().value());
  }

  @PostMapping("/weekly-plans/{planId}/clone")
  @ResponseStatus(HttpStatus.CREATED)
  PlanResponse clonePlan(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID planId,
      @Valid @RequestBody CloneRequest request) {
    return PlanResponse.from(
        cloneWeeklyPlan.clone(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new WeeklyPlanId(planId),
            request.weekStart()));
  }

  @PostMapping("/weekly-plans")
  @ResponseStatus(HttpStatus.CREATED)
  PlanResponse generate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody GenerateRequest request) {
    Set<MemberId> diners =
        request.diners() == null
            ? Set.of()
            : request.diners().stream().map(MemberId::new).collect(Collectors.toSet());
    return PlanResponse.from(
        generateWeeklyPlan.generate(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            request.servings() == null ? 2 : request.servings(),
            diners));
  }

  @GetMapping("/weekly-plans/current")
  PlanResponse current(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return PlanResponse.from(
        viewCurrentPlan.view(CurrentUser.of(jwt), new HouseholdId(householdId)));
  }

  @PatchMapping("/plan-entries/{entryId}")
  EntryResponse change(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID entryId,
      @Valid @RequestBody ChangeRequest request) {
    PlanEntry entry =
        changePlanEntry.change(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new PlanEntryId(entryId),
            new RecipeId(request.recipeId()),
            request.servings());
    return EntryResponse.from(entry, null);
  }

  record RecipeRequest(
      @NotBlank String name,
      @Min(1) int servings,
      @Min(1) int minutes,
      @NotEmpty List<@Valid RequirementRequest> requirements,
      @Size(max = 50) List<@Valid StepRequest> steps) {}

  record CloneRequest(@NotNull LocalDate weekStart) {}

  record GenerateRequest(@Min(1) @Max(20) Integer servings, List<UUID> diners) {}

  record ChangeRequest(@NotNull UUID recipeId, @Min(1) @Max(20) int servings) {}

  record RequirementResponse(String food, BigDecimal grams, boolean optional) {}

  record StepResponse(
      int position,
      String instruction,
      Long timerSeconds,
      String weighFood,
      BigDecimal weighGrams) {

    static StepResponse from(RecipeStep step) {
      return new StepResponse(
          step.position(),
          step.instruction(),
          step.timerDuration().map(Duration::toSeconds).orElse(null),
          step.weighingTarget().map(weighing -> weighing.food().name()).orElse(null),
          step.weighingTarget().map(weighing -> weighing.target().value()).orElse(null));
    }
  }

  record RecipeResponse(
      UUID id,
      String name,
      int servings,
      int minutes,
      List<RequirementResponse> requirements,
      List<StepResponse> steps,
      UUID clonedFrom) {

    static RecipeResponse from(Recipe recipe) {
      return from(recipe, null);
    }

    static RecipeResponse from(Recipe recipe, UUID clonedFrom) {
      return new RecipeResponse(
          recipe.id().value(),
          recipe.name(),
          recipe.servings(),
          recipe.minutes(),
          recipe.requirements().stream()
              .map(
                  requirement ->
                      new RequirementResponse(
                          requirement.food().name(),
                          requirement.grams().value(),
                          requirement.optional()))
              .toList(),
          recipe.steps().stream().map(StepResponse::from).toList(),
          clonedFrom);
    }
  }

  record EntryResponse(
      UUID id,
      int day,
      LocalDate date,
      Meal meal,
      UUID recipeId,
      String recipe,
      int servings,
      boolean needsShopping) {

    static EntryResponse from(PlanEntry entry, LocalDate date) {
      return new EntryResponse(
          entry.id().value(),
          entry.day(),
          date,
          entry.meal(),
          entry.recipe().id().value(),
          entry.recipe().name(),
          entry.servings(),
          entry.needsShopping());
    }
  }

  record PlanResponse(
      UUID id,
      LocalDate weekStart,
      LocalDate weekEnd,
      UUID clonedFrom,
      List<EntryResponse> entries) {

    static PlanResponse from(WeeklyPlan plan) {
      return new PlanResponse(
          plan.id().value(),
          plan.weekStart(),
          plan.weekEnd(),
          plan.clonedFrom().map(WeeklyPlanId::value).orElse(null),
          plan.entries().stream()
              .map(entry -> EntryResponse.from(entry, plan.dateOf(entry)))
              .toList());
    }
  }
}
