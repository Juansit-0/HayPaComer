package dev.haypacomer.agent.kitchen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.application.inventory.EstimateExpiry;
import dev.haypacomer.application.inventory.ExpiryDesk;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.expiry.ShelfLife;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EstimateExpiryToolTest {

  private static final Instant NOW = Instant.parse("2026-10-09T15:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final UserId juan = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
  private final EstimateExpiryTool tool =
      new EstimateExpiryTool(
          new EstimateExpiry(
              households,
              stores.catalog,
              ExpiryDesk.enforcing(food -> new ShelfLife(2, 1, 270, 2), FixedPolicies.DEFAULT),
              Clock.fixed(NOW, ZoneOffset.UTC)));

  EstimateExpiryToolTest() {
    households.save(home);
    stores.catalog.save(
        new FoodMetadata(
            "Chicken breast",
            FoodCategory.POULTRY,
            Unit.GRAM,
            ConversionFactors.MASS_ONLY,
            true,
            2,
            Set.of()));
  }

  private ToolInvocation ask(Map<String, String> arguments) {
    return new ToolInvocation(home.id(), juan, arguments);
  }

  @Test
  void answersWhenFoodExpiresByWhereItIsKept() {
    assertEquals(
        "Chicken breast in the fridge: about 2 days, until 2026-10-11 (usual shelf life; the"
            + " printed date wins when it is sooner)",
        tool.invoke(ask(Map.of("food", "Chicken breast"))).content());
    assertTrue(
        tool.invoke(ask(Map.of("food", "chicken breast", "place", "congelador")))
            .content()
            .contains("about 270 days, until 2027-07-06"));
    assertTrue(
        tool.invoke(ask(Map.of("food", "Chicken breast", "place", "door", "opened", "sí")))
            .content()
            .startsWith("Chicken breast opened in the door: about 1 days"));
    assertEquals("estimate_expiry", tool.spec().name());
    assertEquals(
        "Estimate when Chicken breast expires",
        tool.describe(ask(Map.of("food", "Chicken breast"))));
  }

  @Test
  void rejectsUnknownPlacesBeforeRunning() {
    assertTrue(tool.problem(ask(Map.of("food", "Chicken breast", "place", "oven"))).isPresent());
    assertTrue(tool.problem(ask(Map.of("food", "Chicken breast"))).isEmpty());
  }
}
