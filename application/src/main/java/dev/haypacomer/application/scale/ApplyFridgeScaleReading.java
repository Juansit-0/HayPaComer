package dev.haypacomer.application.scale;

import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.application.inventory.ConsumeFoodCommand;
import dev.haypacomer.application.inventory.ExecuteInventoryCommand;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.FridgeSessionRegistry;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.ScaleAssignmentRepository;
import dev.haypacomer.application.sensor.WeightReadingHandler;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.WeightReading;
import java.util.Objects;
import java.util.Optional;

public final class ApplyFridgeScaleReading implements WeightReadingHandler {

  private final ScaleAssignmentRepository assignments;
  private final FridgeRepository fridges;
  private final ExecuteInventoryCommand commands;
  private final FridgeSessionRegistry sessions;
  private final PolicySource policies;

  public ApplyFridgeScaleReading(
      ScaleAssignmentRepository assignments,
      FridgeRepository fridges,
      ExecuteInventoryCommand commands,
      FridgeSessionRegistry sessions,
      Grams minimumChange) {
    this(
        assignments,
        fridges,
        commands,
        sessions,
        FixedPolicies.DEFAULT.withThresholds(
            FridgeThresholds.DEFAULT.withMinimumWeightChange(minimumChange.value())));
  }

  public ApplyFridgeScaleReading(
      ScaleAssignmentRepository assignments,
      FridgeRepository fridges,
      ExecuteInventoryCommand commands,
      FridgeSessionRegistry sessions,
      PolicySource policies) {
    this.assignments = Objects.requireNonNull(assignments, "assignments");
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.commands = Objects.requireNonNull(commands, "commands");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.policies = Objects.requireNonNull(policies, "policies");
  }

  @Override
  public Optional<CommandOutcome> apply(Device scale, WeightReading reading) {
    if (reading.mode() != ScaleMode.FRIDGE || !reading.stable()) {
      return Optional.empty();
    }
    return assignments
        .find(scale.id())
        .flatMap(
            assignment ->
                fridges.findByHousehold(assignment.household()).stream()
                    .flatMap(fridge -> fridge.findItem(assignment.item()).stream())
                    .findFirst()
                    .flatMap(item -> discount(scale, assignment, item, reading)));
  }

  private Optional<CommandOutcome> discount(
      Device scale, ScaleAssignment assignment, FoodItem item, WeightReading reading) {
    Grams measuredNet =
        reading.grams().shortfallTo(item.tare()).isZero()
            ? reading.grams().minus(item.tare())
            : Grams.ZERO;
    Grams consumed = measuredNet.shortfallTo(item.quantity());
    Grams minimumChange =
        Grams.of(policies.thresholds(assignment.household()).minimumWeightChange());
    if (consumed.compareTo(minimumChange) < 0) {
      return Optional.empty();
    }
    ConsumeFoodCommand command =
        new ConsumeFoodCommand(
            reading.id().value(),
            assignment.household(),
            item.id(),
            consumed,
            MovementSource.SCALE);
    Optional<UserId> atTheFridge =
        sessions.sessionOf(scale.fridge()).activeUser(reading.occurredAt());
    if (atTheFridge.isPresent()) {
      try {
        return Optional.of(commands.execute(atTheFridge.get(), command));
      } catch (AccessDeniedException notTheirs) {
        return Optional.of(commands.execute(assignment.assignedBy(), command));
      }
    }
    return Optional.of(commands.execute(assignment.assignedBy(), command));
  }
}
