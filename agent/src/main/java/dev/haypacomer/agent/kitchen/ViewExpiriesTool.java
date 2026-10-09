package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ParameterType;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.domain.inventory.FoodStatus;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class ViewExpiriesTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read(
          "view_expiries",
          "Food to use first: expired, at risk, or expiring within the given days, rescue order.",
          List.of(ParameterSpec.optional("days", ParameterType.COUNT, "Days ahead, default 3")));

  private final ViewInventory inventory;
  private final KitchenToday today;

  public ViewExpiriesTool(ViewInventory inventory, KitchenToday today) {
    this.inventory = inventory;
    this.today = today;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Read upcoming expiries";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    int days = Integer.parseInt(invocation.arguments().getOrDefault("days", "3").strip());
    LocalDate now = today.of(invocation.household());
    LocalDate horizon = now.plusDays(days);
    List<InventoryEntry> soon =
        inventory.view(invocation.user(), invocation.household(), now).stream()
            .filter(InventoryEntry::usable)
            .filter(
                entry ->
                    entry.food().has(FoodStatus.EXPIRED)
                        || entry.food().has(FoodStatus.AT_RISK)
                        || entry
                            .food()
                            .item()
                            .expiresOn()
                            .map(date -> !date.isAfter(horizon))
                            .orElse(false))
            .sorted(
                Comparator.comparingInt((InventoryEntry entry) -> entry.food().rescuePriority())
                    .reversed())
            .toList();
    return Observation.of(
        SPEC.name(),
        InventoryLines.describe(soon, "Nothing expires in the next " + days + " days"));
  }
}
