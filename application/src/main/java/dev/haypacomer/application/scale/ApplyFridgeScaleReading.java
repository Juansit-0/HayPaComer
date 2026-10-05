package dev.haypacomer.application.scale;

import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.application.inventory.ConsumeFoodCommand;
import dev.haypacomer.application.inventory.ExecuteInventoryCommand;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.ScaleAssignmentRepository;
import dev.haypacomer.application.sensor.WeightReadingHandler;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.WeightReading;
import java.util.Objects;
import java.util.Optional;

public final class ApplyFridgeScaleReading implements WeightReadingHandler {

  private final ScaleAssignmentRepository assignments;
  private final FridgeRepository fridges;
  private final ExecuteInventoryCommand commands;
  private final Grams minimumChange;

  public ApplyFridgeScaleReading(
      ScaleAssignmentRepository assignments,
      FridgeRepository fridges,
      ExecuteInventoryCommand commands,
      Grams minimumChange) {
    this.assignments = Objects.requireNonNull(assignments, "assignments");
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.commands = Objects.requireNonNull(commands, "commands");
    this.minimumChange = Objects.requireNonNull(minimumChange, "minimumChange");
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
                    .flatMap(item -> discount(assignment, item, reading)));
  }

  private Optional<CommandOutcome> discount(
      ScaleAssignment assignment, FoodItem item, WeightReading reading) {
    Grams measuredNet =
        reading.grams().shortfallTo(item.tare()).isZero()
            ? reading.grams().minus(item.tare())
            : Grams.ZERO;
    Grams consumed = measuredNet.shortfallTo(item.quantity());
    if (consumed.compareTo(minimumChange) < 0) {
      return Optional.empty();
    }
    return Optional.of(
        commands.execute(
            assignment.assignedBy(),
            new ConsumeFoodCommand(
                reading.id().value(),
                assignment.household(),
                item.id(),
                consumed,
                MovementSource.SCALE)));
  }
}
