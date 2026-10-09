package dev.haypacomer.application.coldchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.SensorHistory;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.coldchain.investigation.ColdInvestigation;
import dev.haypacomer.domain.coldchain.investigation.FoodVerdict;
import dev.haypacomer.domain.coldchain.investigation.LikelyCause;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvestigateColdIncidentsTest {

  private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final UserId owner = UserId.newId();
  private final UserId ana = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneOffset.UTC, owner, NOW);
  private final List<SensorEvent> events = new ArrayList<>();
  private final SensorHistory history =
      (fridge, from, to) ->
          events.stream()
              .filter(event -> event.fridge().equals(fridge))
              .filter(
                  event -> !event.occurredAt().isBefore(from) && event.occurredAt().isBefore(to))
              .toList();
  private final Fridge fridge;
  private final InvestigateColdIncidents investigate;

  {
    home.join(ana, Role.MEMBER, NOW);
    households.save(home);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(owner, home.id(), "Kitchen", FridgeLayout.STANDARD);
    investigate =
        new InvestigateColdIncidents(
            households,
            fridges,
            history,
            new ViewInventory(
                households,
                fridges,
                stores.ownerships,
                new InMemoryColdChainRepository(),
                FreshnessPolicy.DEFAULT),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private FoodItem put(String name) {
    FoodItem item =
        new FoodItem(
            FoodItemId.newId(),
            new FoodMetadata(
                name,
                FoodCategory.OTHER,
                Unit.GRAM,
                ConversionFactors.MASS_ONLY,
                true,
                5,
                Set.of()),
            Grams.of(300),
            Grams.ZERO,
            null);
    fridge.place(item, fridge.trays().findFirst().orElseThrow().id());
    return item;
  }

  private void temp(int minutesAgo, String celsius) {
    events.add(
        new TemperatureReading(
            new SensorEventId(UUID.randomUUID()),
            DeviceId.newId(),
            fridge.id(),
            NOW.minus(Duration.ofMinutes(minutesAgo)),
            new BigDecimal(celsius)));
  }

  @Test
  void investigatesTheLastHoursAndHidesOtherMembersPrivateFood() {
    put("Chicken breast");
    FoodItem secret = put("Cake");
    stores.ownerships.save(
        secret.id(),
        Ownership.of(home.membershipOf(owner).orElseThrow().member(), Visibility.PRIVATE));
    temp(200, "4.0");
    temp(180, "7.0");
    temp(170, "7.0");
    temp(40, "4.0");
    temp(60 * 30, "9.0");

    List<ColdInvestigation> found =
        investigate.investigate(ana, home.id(), Optional.empty(), Duration.ofHours(24));

    ColdInvestigation result = found.getFirst();
    assertEquals(4, result.readings());
    assertEquals(LikelyCause.READINGS_MISSING, result.episodes().getFirst().cause());
    assertEquals(FoodVerdict.DISCARD, result.foods().getFirst().verdict());
    assertEquals("Private food", result.foods().get(1).food());
    assertEquals(Grams.ZERO, result.foods().get(1).grams());
    assertEquals(FoodVerdict.DISCARD, result.foods().get(1).verdict());
    assertEquals(
        found,
        investigate.investigate(ana, home.id(), Optional.of(fridge.id()), Duration.ofHours(24)));
  }

  @Test
  void checksMembershipFridgeAndWindow() {
    assertThrows(
        HouseholdNotFoundException.class,
        () ->
            investigate.investigate(
                UserId.newId(), home.id(), Optional.empty(), Duration.ofHours(1)));
    assertThrows(
        FridgeNotFoundException.class,
        () ->
            investigate.investigate(
                owner, home.id(), Optional.of(FridgeId.newId()), Duration.ofHours(1)));
    assertThrows(
        IllegalArgumentException.class,
        () -> investigate.investigate(owner, home.id(), Optional.empty(), Duration.ofDays(8)));
    assertThrows(
        IllegalArgumentException.class,
        () -> investigate.investigate(owner, home.id(), Optional.empty(), Duration.ZERO));
  }
}
