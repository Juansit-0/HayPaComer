package dev.haypacomer.domain.recipe;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record RecipeStep(int position, String instruction, Duration timer, StepWeighing weighing) {

  public RecipeStep {
    if (position < 1) {
      throw new IllegalArgumentException("Step position starts at 1: " + position);
    }
    Objects.requireNonNull(instruction, "instruction");
    instruction = instruction.strip();
    if (instruction.isEmpty()) {
      throw new IllegalArgumentException("Step instruction cannot be blank");
    }
    if (timer != null && (timer.isNegative() || timer.isZero())) {
      throw new IllegalArgumentException("Step timer must be positive: " + timer);
    }
  }

  public static RecipeStep of(int position, String instruction) {
    return new RecipeStep(position, instruction, null, null);
  }

  public RecipeStep withTimer(Duration duration) {
    return new RecipeStep(position, instruction, duration, weighing);
  }

  public RecipeStep withWeighing(StepWeighing stepWeighing) {
    return new RecipeStep(position, instruction, timer, stepWeighing);
  }

  public Optional<Duration> timerDuration() {
    return Optional.ofNullable(timer);
  }

  public Optional<StepWeighing> weighingTarget() {
    return Optional.ofNullable(weighing);
  }

  public RecipeStep scaledBy(BigDecimal factor) {
    return weighing == null ? this : withWeighing(weighing.scaledBy(factor));
  }
}
