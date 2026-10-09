package dev.haypacomer.agent.copilot;

import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.application.live.BroadcastLiveUpdate;
import dev.haypacomer.application.live.LiveUpdate;
import dev.haypacomer.application.live.LiveUpdateKind;
import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.application.sensor.WeightReadingHandler;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.scale.WeighingStatus;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.WeightReading;
import dev.haypacomer.domain.session.CookingSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ScaleCopilot implements WeightReadingHandler {

  static final int MAX_ADJUSTED = 3;

  private final ScaleSessionStore scales;
  private final CookingSessionRepository sessions;
  private final BroadcastLiveUpdate live;
  private final Map<DeviceId, String> lastHint = new ConcurrentHashMap<>();

  public ScaleCopilot(
      ScaleSessionStore scales, CookingSessionRepository sessions, BroadcastLiveUpdate live) {
    this.scales = scales;
    this.sessions = sessions;
    this.live = live;
  }

  @Override
  public Optional<CommandOutcome> apply(Device scale, WeightReading reading) {
    if (reading.mode() != ScaleMode.COOKING || !reading.stable()) {
      return Optional.empty();
    }
    Optional<WeighingTarget> target = scales.target(scale.id());
    if (target.isEmpty()) {
      return Optional.empty();
    }
    WeighingProgress progress = target.get().evaluate(reading.grams());
    Optional<CookingSession> session =
        sessions
            .active(scale.household())
            .filter(active -> active.scale().equals(Optional.of(scale.id())));
    String hint = hint(progress, session);
    String key =
        progress.food()
            + progress.status()
            + (progress.status() == WeighingStatus.SHORT ? progress.percent() / 10 : "")
            + (progress.status() == WeighingStatus.OVER ? progress.measured() : "");
    if (!key.equals(lastHint.put(scale.id(), key))) {
      live.publish(
          LiveUpdate.of(
              scale.household(),
              LiveUpdateKind.COPILOT,
              scale.fridge().value(),
              hint,
              reading.occurredAt()));
    }
    return Optional.empty();
  }

  static String hint(WeighingProgress progress, Optional<CookingSession> session) {
    String food = progress.food();
    return switch (progress.status()) {
      case SHORT ->
          "Add "
              + whole(progress.remaining())
              + " more "
              + food
              + " ("
              + progress.percent()
              + "%).";
      case ON_TARGET ->
          food + " is on target at " + whole(progress.measured()) + ". Confirm the step to go on.";
      case OVER -> over(progress, session);
    };
  }

  private static String over(WeighingProgress progress, Optional<CookingSession> session) {
    Grams extra = progress.measured().minus(progress.target());
    String base =
        whole(extra)
            + " over on "
            + progress.food()
            + " ("
            + whole(progress.measured())
            + " of "
            + whole(progress.target())
            + "). Take "
            + whole(extra)
            + " out";
    BigDecimal factor =
        progress.measured().value().divide(progress.target().value(), 4, RoundingMode.HALF_UP);
    List<String> adjusted =
        session.stream()
            .flatMap(active -> active.recipe().mandatoryRequirements().stream())
            .filter(requirement -> !requirement.food().name().equalsIgnoreCase(progress.food()))
            .limit(MAX_ADJUSTED)
            .map(requirement -> scaled(requirement, factor))
            .toList();
    if (adjusted.isEmpty()) {
      return base + ".";
    }
    return base
        + ", or keep it and scale the rest by "
        + factor.setScale(2, RoundingMode.HALF_UP).toPlainString()
        + ": "
        + adjusted.stream().collect(Collectors.joining(", "))
        + ".";
  }

  private static String scaled(RecipeRequirement requirement, BigDecimal factor) {
    return requirement.food().name()
        + " "
        + whole(requirement.grams().times(factor))
        + " instead of "
        + whole(requirement.grams());
  }

  private static String whole(Grams grams) {
    return grams.value().setScale(0, RoundingMode.HALF_UP).toPlainString() + " g";
  }
}
