package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ParameterType;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import java.util.List;
import java.util.Locale;

public final class QueryInventoryTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read(
          "query_inventory",
          "Measured food in the household fridges with grams, expiry, and status.",
          List.of(ParameterSpec.optional("food", ParameterType.TEXT, "Filter by food name")));

  private final ViewInventory inventory;
  private final KitchenToday today;

  public QueryInventoryTool(ViewInventory inventory, KitchenToday today) {
    this.inventory = inventory;
    this.today = today;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Read the inventory";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    String filter =
        invocation.arguments().getOrDefault("food", "").strip().toLowerCase(Locale.ROOT);
    List<InventoryEntry> entries =
        inventory
            .view(invocation.user(), invocation.household(), today.of(invocation.household()))
            .stream()
            .filter(
                entry ->
                    filter.isEmpty()
                        || (entry.usable()
                            && entry
                                .food()
                                .item()
                                .name()
                                .toLowerCase(Locale.ROOT)
                                .contains(filter)))
            .toList();
    return Observation.of(SPEC.name(), InventoryLines.describe(entries, "No food found"));
  }
}
