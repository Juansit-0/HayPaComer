package dev.haypacomer.domain.session;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.scale.WeighingTarget;
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
  private Grams measured;

  private CookingSession(
      CookingSessionId id,
      HouseholdId household,
      Recipe recipe,
      UserId startedBy,
      Instant startedAt,
      SessionState state,
      Instant updatedAt,
      List<StepCompletion> completions,
      DeviceId scale,
      Grams measured) {
    this.id = Objects.requireNonNull(id, "id");
    this.household = Objects.requireNonNull(household, "household");
    this.recipe = Objects.requireNonNull(recipe, "recipe");
    this.startedBy = Objects.requireNonNull(startedBy, "startedBy");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.state = Objects.requireNonNull(state, "state");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.completions = new ArrayList<>(completions);
    this.scale = scale;
    this.measured = measured;
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
        null,
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
      DeviceId scale,
      Grams measured) {
    return new CookingSession(
        id,
        household,
        recipe,
        startedBy,
        startedAt,
        state,
        updatedAt,
        completions,
        scale,
        measured);
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

  public Optional<Grams> measured() {
    return Optional.ofNullable(measured);
  }

  public WeighingTarget weighingTargetFor(int position) {
    if (phase() != SessionPhase.COOKING || position != state.currentStep()) {
      throw new IllegalSessionTransitionException(phase(), "weigh step " + position + " of");
    }
    StepWeighing weighing =
        step()
            .flatMap(RecipeStep::weighingTarget)
            .orElseThrow(
                () -> new IllegalArgumentException("Step " + position + " has nothing to weigh"));
    return WeighingTarget.of(weighing.food().name(), weighing.target());
  }

  public WeighingProgress weigh(int position, Grams grams, Instant at) {
    Objects.requireNonNull(grams, "grams");
    WeighingProgress progress = weighingTargetFor(position).evaluate(grams);
    measured = grams;
    updatedAt = at;
    return progress;
  }

  public List<StepCompletion> completions() {
    return List.copyOf(completions);
  }

  public void next(Instant at) {
    SessionState before = state;
    state = state.next(recipe.steps().size(), at);
    if (before.phase() == SessionPhase.COOKING) {
      completions.add(new StepCompletion(before.currentStep(), at, measured));
      measured = null;
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
