package dev.haypacomer.domain.session;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeStep;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CookingSession {

  private final CookingSessionId id;
  private final HouseholdId household;
  private final Recipe recipe;
  private final UserId startedBy;
  private final Instant startedAt;
  private final List<StepCompletion> completions;
  private SessionState state;
  private Instant updatedAt;
  private DeviceId scale;

  private CookingSession(
      CookingSessionId id,
      HouseholdId household,
      Recipe recipe,
      UserId startedBy,
      Instant startedAt,
      SessionState state,
      Instant updatedAt,
      List<StepCompletion> completions,
      DeviceId scale) {
    this.id = Objects.requireNonNull(id, "id");
    this.household = Objects.requireNonNull(household, "household");
    this.recipe = Objects.requireNonNull(recipe, "recipe");
    this.startedBy = Objects.requireNonNull(startedBy, "startedBy");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.state = Objects.requireNonNull(state, "state");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.completions = new ArrayList<>(completions);
    this.scale = scale;
    if (state.currentStep() > recipe.steps().size()) {
      throw new IllegalArgumentException("Step " + state.currentStep() + " is beyond the recipe");
    }
  }

  public static CookingSession start(
      HouseholdId household, Recipe recipe, int servings, UserId startedBy, Instant at) {
    return new CookingSession(
        CookingSessionId.newId(),
        household,
        recipe.scaledTo(servings),
        startedBy,
        at,
        new Preparing(),
        at,
        List.of(),
        null);
  }

  public static CookingSession restore(
      CookingSessionId id,
      HouseholdId household,
      Recipe recipe,
      UserId startedBy,
      Instant startedAt,
      SessionState state,
      Instant updatedAt,
      List<StepCompletion> completions,
      DeviceId scale) {
    return new CookingSession(
        id, household, recipe, startedBy, startedAt, state, updatedAt, completions, scale);
  }

  public CookingSessionId id() {
    return id;
  }

  public HouseholdId household() {
    return household;
  }

  public Recipe recipe() {
    return recipe;
  }

  public UserId startedBy() {
    return startedBy;
  }

  public Instant startedAt() {
    return startedAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public SessionState state() {
    return state;
  }

  public SessionPhase phase() {
    return state.phase();
  }

  public int currentStep() {
    return state.currentStep();
  }

  public Optional<DeviceId> scale() {
    return Optional.ofNullable(scale);
  }

  public void useScale(DeviceId device) {
    Objects.requireNonNull(device, "device");
    if (!phase().active()) {
      throw new IllegalSessionTransitionException(phase(), "attach a scale to");
    }
    scale = device;
  }

  public Optional<RecipeStep> step() {
    int position = state.currentStep();
    return position == 0 ? Optional.empty() : Optional.of(recipe.steps().get(position - 1));
  }

  public List<StepCompletion> completions() {
    return List.copyOf(completions);
  }

  public void next(Instant at) {
    SessionState before = state;
    state = state.next(recipe.steps().size(), at);
    if (before.phase() == SessionPhase.COOKING) {
      completions.add(new StepCompletion(before.currentStep(), at));
    }
    updatedAt = at;
  }

  public void pause(Instant at) {
    state = state.pause(at);
    updatedAt = at;
  }

  public void resume(Instant at) {
    state = state.resume(at);
    updatedAt = at;
  }

  public void abandon(Instant at) {
    state = state.abandon(at);
    updatedAt = at;
  }
}
