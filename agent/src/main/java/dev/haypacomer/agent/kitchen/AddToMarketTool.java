package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ParameterType;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.market.AddToMarketList;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.market.MarketItem;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.Grams;
import java.util.List;
import java.util.Optional;

public final class AddToMarketTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.write(
          "add_to_market",
          "Propose adding a catalog food to the market list after the person confirms it.",
          Permission.MANAGE_MARKET_LIST,
          List.of(
              ParameterSpec.required("food", ParameterType.TEXT, "Catalog food name"),
              ParameterSpec.required("grams", ParameterType.GRAMS, "Grams to buy")));

  private final AddToMarketList add;
  private final FoodCatalogRepository catalog;

  public AddToMarketTool(AddToMarketList add, FoodCatalogRepository catalog) {
    this.add = add;
    this.catalog = catalog;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public Optional<String> problem(ToolInvocation invocation) {
    String food = invocation.arguments().get("food").strip();
    return catalog.findByName(food).isPresent()
        ? Optional.empty()
        : Optional.of("Unknown food " + food + "; use a catalog name");
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Add "
        + Grams.of(invocation.arguments().get("grams").strip())
        + " of "
        + invocation.arguments().get("food").strip()
        + " to the market list";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    MarketItem item =
        add.add(
            invocation.user(),
            invocation.household(),
            invocation.arguments().get("food").strip(),
            Grams.of(invocation.arguments().get("grams").strip()),
            MarketSource.AGENT_CONFIRMED);
    return Observation.of(
        SPEC.name(), "Added " + item.grams() + " of " + item.food().name() + " to the market list");
  }
}
