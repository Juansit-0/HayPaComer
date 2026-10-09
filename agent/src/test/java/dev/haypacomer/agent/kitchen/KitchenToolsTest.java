package dev.haypacomer.agent.kitchen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.planning.Meal;
import dev.haypacomer.domain.planning.PlanEntry;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KitchenToolsTest {

  private final KitchenFixture kitchen = new KitchenFixture();

  private ToolInvocation call(Map<String, String> arguments) {
    return new ToolInvocation(kitchen.home.id(), kitchen.owner, arguments);
  }

  @Test
  void inventoryListsMeasuredFoodAndFilters() {
    QueryInventoryTool tool = new QueryInventoryTool(kitchen.inventory, kitchen.today);
    assertEquals("No food found", tool.invoke(call(Map.of())).content());
    kitchen.put("Chicken breast", 650, KitchenFixture.TODAY.plusDays(1));
    kitchen.put("Rice", 900, null);

    String all = tool.invoke(call(Map.of())).content();
    assertTrue(all.contains("Chicken breast 650 g, expires 2026-10-10"));
    assertTrue(all.contains("Rice 900 g"));
    assertEquals("Rice 900 g", tool.invoke(call(Map.of("food", " rice "))).content());
    assertEquals("Read the inventory", tool.describe(call(Map.of())));
  }

  @Test
  void longInventoriesAreCut() {
    for (int i = 0; i < 45; i++) {
      kitchen.put("Food " + i, 100, null);
    }

    String content =
        new QueryInventoryTool(kitchen.inventory, kitchen.today).invoke(call(Map.of())).content();

    assertTrue(content.endsWith("... 5 more"));
  }

  @Test
  void expiriesComeInRescueOrder() {
    ViewExpiriesTool tool = new ViewExpiriesTool(kitchen.inventory, kitchen.today);
    assertEquals("Nothing expires in the next 3 days", tool.invoke(call(Map.of())).content());
    kitchen.put("Yogurt", 200, KitchenFixture.TODAY.minusDays(1));
    kitchen.put("Milk", 900, KitchenFixture.TODAY.plusDays(2));
    kitchen.put("Rice", 900, null);
    kitchen.put("Cheese", 300, KitchenFixture.TODAY.plusDays(6));

    List<String> lines = List.of(tool.invoke(call(Map.of())).content().split("\n"));

    assertEquals(2, lines.size());
    assertTrue(lines.stream().noneMatch(line -> line.startsWith("Rice")));
    assertTrue(tool.invoke(call(Map.of("days", "7"))).content().contains("Cheese"));
    assertEquals("Read upcoming expiries", tool.describe(call(Map.of())));
  }

  @Test
  void marketToolsReadAndAddCatalogFood() {
    ViewMarketListTool view = new ViewMarketListTool(kitchen.viewMarket());
    AddToMarketTool add = new AddToMarketTool(kitchen.addToMarket(), kitchen.stores.catalog);
    kitchen.stores.catalog.save(KitchenFixture.food("Milk"));
    assertEquals("The market list is empty", view.invoke(call(Map.of())).content());

    ToolInvocation milk = call(Map.of("food", "milk", "grams", "1000"));
    assertEquals(Optional.empty(), add.problem(milk));
    assertEquals("Add 1000 g of milk to the market list", add.describe(milk));
    assertEquals("Added 1000 g of Milk to the market list", add.invoke(milk).content());
    assertEquals("OTHER: Milk 1000 g", view.invoke(call(Map.of())).content());
    assertEquals(
        Optional.of("Unknown food caviar; use a catalog name"),
        add.problem(call(Map.of("food", "caviar", "grams", "50"))));
    assertEquals("Read the market list", view.describe(call(Map.of())));
  }

  @Test
  void coldChainShowsPhaseAndReview() {
    ViewColdChainTool tool = new ViewColdChainTool(kitchen.coldChains());
    assertTrue(tool.invoke(call(Map.of())).content().endsWith(": NORMAL, no reading"));
    ColdChain chain = ColdChain.start(kitchen.fridge.id());
    chain.record(new BigDecimal("4.1"), KitchenFixture.NOW, FridgeThresholds.DEFAULT);
    kitchen.chains.save(chain);

    String content = tool.invoke(call(Map.of())).content();

    assertTrue(content.contains("4.1 C at 2026-10-09T18:00:00Z"));
    assertTrue(content.startsWith("Fridge " + kitchen.fridge.id().value()));
    assertEquals("Read the cold chain", tool.describe(call(Map.of())));
  }

  @Test
  void weeklyPlanIsReadDayByDay() {
    ViewWeeklyPlanTool tool = new ViewWeeklyPlanTool(kitchen.currentPlan());
    assertEquals("No plan for this week", tool.invoke(call(Map.of())).content());
    Recipe rice =
        new Recipe(RecipeId.newId(), "Rice bowl", 2, 20, RecipeSource.MANUAL, List.of(), List.of());
    kitchen.planning.plans.save(
        WeeklyPlan.create(
            kitchen.home.id(),
            KitchenFixture.TODAY.minusDays(4),
            List.of(PlanEntry.of(5, Meal.DINNER, rice, 2, true))));

    assertEquals(
        "Week of 2026-10-05\n2026-10-09 DINNER: Rice bowl for 2, needs shopping",
        tool.invoke(call(Map.of())).content());
    assertEquals("Read the weekly plan", tool.describe(call(Map.of())));
  }

  @Test
  void theBudgetToolExplainsWhatFitsAndCheaperOptions() {
    java.util.Map<dev.haypacomer.domain.household.HouseholdId, java.math.BigDecimal> budgets =
        new java.util.HashMap<>();
    dev.haypacomer.domain.food.FoodMetadata beef = KitchenFixture.food("Ground beef");
    dev.haypacomer.domain.food.FoodMetadata chicken = KitchenFixture.food("Chicken breast");
    dev.haypacomer.domain.food.FoodMetadata saffron = KitchenFixture.food("Saffron");
    kitchen.stores.catalog.save(beef);
    kitchen.stores.catalog.save(chicken);
    kitchen.stores.catalog.save(saffron);
    ReviewBudgetTool tool =
        new ReviewBudgetTool(
            new dev.haypacomer.application.market.ViewMarketBudget(
                kitchen.households,
                new dev.haypacomer.application.port.MarketBudgetRepository() {
                  @Override
                  public java.util.Optional<java.math.BigDecimal> monthly(
                      dev.haypacomer.domain.household.HouseholdId household) {
                    return java.util.Optional.ofNullable(budgets.get(household));
                  }

                  @Override
                  public void save(
                      dev.haypacomer.domain.household.HouseholdId household,
                      java.math.BigDecimal monthly) {
                    budgets.put(household, monthly);
                  }
                },
                kitchen.marketLists,
                new dev.haypacomer.application.port.FoodPriceRepository() {
                  @Override
                  public java.util.Map<String, java.math.BigDecimal> pricesFor(
                      dev.haypacomer.domain.household.HouseholdId household,
                      java.util.Currency currency) {
                    return java.util.Map.of(
                        "ground beef", new java.math.BigDecimal("30000"),
                        "chicken breast", new java.math.BigDecimal("22000"));
                  }

                  @Override
                  public void save(
                      dev.haypacomer.domain.household.HouseholdId household,
                      String foodKey,
                      java.math.BigDecimal pricePerKg) {}
                },
                new dev.haypacomer.application.port.SubstitutionRuleRepository() {
                  @Override
                  public void save(dev.haypacomer.domain.substitution.SubstitutionRule rule) {}

                  @Override
                  public List<dev.haypacomer.domain.substitution.SubstitutionRule> all() {
                    return List.of(
                        dev.haypacomer.domain.substitution.SubstitutionRule.of(
                            beef, chicken, "1", 1000));
                  }
                },
                kitchen.clock));
    assertEquals("No monthly market budget yet", tool.invoke(call(Map.of())).content());
    kitchen
        .addToMarket()
        .add(
            kitchen.owner,
            kitchen.home.id(),
            "Ground beef",
            dev.haypacomer.domain.quantity.Grams.of(500),
            dev.haypacomer.domain.market.MarketSource.MANUAL);
    kitchen
        .addToMarket()
        .add(
            kitchen.owner,
            kitchen.home.id(),
            "Saffron",
            dev.haypacomer.domain.quantity.Grams.of(5),
            dev.haypacomer.domain.market.MarketSource.MANUAL);
    budgets.put(kitchen.home.id(), new java.math.BigDecimal("10000"));

    String content = tool.invoke(call(Map.of())).content();

    assertTrue(content.startsWith("Budget 10000.00 COP, spent 0.00, left 10000.00"));
    assertTrue(
        content.contains(
            "Does not fit: Ground beef 500 g for 15000.00 (cheaper: Chicken breast 500 g for"
                + " 11000.00)"));
    assertTrue(content.contains("No price yet: Saffron"));
    assertEquals("Review the market budget", tool.describe(call(Map.of())));
  }
}
