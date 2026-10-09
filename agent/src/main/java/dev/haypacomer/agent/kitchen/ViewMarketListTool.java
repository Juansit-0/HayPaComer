package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.market.ViewMarketList;
import dev.haypacomer.domain.market.MarketItem;
import java.util.List;
import java.util.stream.Collectors;

public final class ViewMarketListTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read("view_market_list", "Pending items on the market list by aisle.", List.of());

  private final ViewMarketList market;

  public ViewMarketListTool(ViewMarketList market) {
    this.market = market;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Read the market list";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    var byAisle = market.view(invocation.user(), invocation.household()).pendingByCategory();
    if (byAisle.isEmpty()) {
      return Observation.of(SPEC.name(), "The market list is empty");
    }
    return Observation.of(
        SPEC.name(),
        byAisle.entrySet().stream()
            .map(aisle -> aisle.getKey() + ": " + items(aisle.getValue()))
            .collect(Collectors.joining("\n")));
  }

  private static String items(List<MarketItem> items) {
    return items.stream()
        .map(item -> item.food().name() + " " + item.grams())
        .collect(Collectors.joining(", "));
  }
}
