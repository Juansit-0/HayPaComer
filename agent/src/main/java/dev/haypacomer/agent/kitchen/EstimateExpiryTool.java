package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ParameterType;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.inventory.EstimateExpiry;
import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.fridge.ZoneKind;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class EstimateExpiryTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read(
          "estimate_expiry",
          "Usual expiry of a food by where it is kept (fridge, door, freezer) and whether it"
              + " is opened, from the shelf life rules.",
          List.of(
              ParameterSpec.required("food", ParameterType.TEXT, "Food name from the catalog"),
              ParameterSpec.optional("place", ParameterType.TEXT, "fridge, door, or freezer"),
              ParameterSpec.optional("opened", ParameterType.TEXT, "yes or no, default no")));

  private final EstimateExpiry estimateExpiry;

  public EstimateExpiryTool(EstimateExpiry estimateExpiry) {
    this.estimateExpiry = estimateExpiry;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Estimate when " + invocation.arguments().get("food") + " expires";
  }

  @Override
  public Optional<String> problem(ToolInvocation invocation) {
    String place = invocation.arguments().getOrDefault("place", "fridge");
    return zone(place).isPresent()
        ? Optional.empty()
        : Optional.of("place must be fridge, door, or freezer");
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    String food = invocation.arguments().get("food").strip();
    String place = invocation.arguments().getOrDefault("place", "fridge").strip();
    boolean opened =
        invocation
            .arguments()
            .getOrDefault("opened", "no")
            .strip()
            .toLowerCase(Locale.ROOT)
            .matches("yes|si|sí|true|opened|abierto");
    ExpiryEstimate estimate =
        estimateExpiry.estimate(
            invocation.user(), invocation.household(), food, zone(place).orElseThrow(), opened);
    return Observation.of(
        SPEC.name(),
        food
            + (opened ? " opened" : "")
            + " in the "
            + place.toLowerCase(Locale.ROOT)
            + ": about "
            + estimate.shelfDays()
            + " days, until "
            + estimate.date()
            + " (usual shelf life; the printed date wins when it is sooner)");
  }

  private static Optional<ZoneKind> zone(String place) {
    return switch (place.strip().toLowerCase(Locale.ROOT)) {
      case "fridge", "nevera", "shelf" -> Optional.of(ZoneKind.SHELF);
      case "door", "puerta" -> Optional.of(ZoneKind.DOOR);
      case "freezer", "congelador" -> Optional.of(ZoneKind.FREEZER);
      default -> Optional.empty();
    };
  }
}
