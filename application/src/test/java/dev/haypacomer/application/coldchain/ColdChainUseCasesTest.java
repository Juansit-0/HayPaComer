package dev.haypacomer.application.coldchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.coldchain.ColdChainPhase;
import dev.haypacomer.domain.coldchain.ColdIncident;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FoodStatus;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ColdChainUseCasesTest {

  private static final Instant T0 = Instant.parse("2026-10-03T18:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryColdChainRepository chains = new InMemoryColdChainRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final Clock clock = Clock.fixed(T0.plus(Duration.ofHours(1)), ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId guest = UserId.newId();
  private Household household;
  private Fridge fridge;
  private Device device;
  private TrackColdChain track;

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, T0);
    household.join(guest, Role.GUEST, T0);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    Tray top = fridge.trays().findFirst().orElseThrow();
    fridge.place(
        new FoodItem(
            FoodItemId.newId(),
            food("Chicken breast", true),
            Grams.of(200),
            Grams.ZERO,
            LocalDate.of(2026, 10, 9)),
        top.id());
    fridge.place(
        new FoodItem(FoodItemId.newId(), food("Rice", false), Grams.of(1000), Grams.ZERO, null),
        top.id());
    device =
        Device.register(household.id(), fridge.id(), "Probe", DeviceKind.ESP32_DOOR_TEMP, "ab", T0);
    track = new TrackColdChain(chains, FridgeThresholds.DEFAULT);
  }

  private static FoodMetadata food(String name, boolean perishable) {
    return new FoodMetadata(
        name, FoodCategory.OTHER, Unit.GRAM, ConversionFactors.MASS_ONLY, perishable, 3, Set.of());
  }

  private ColdChainPhase reading(String celsius, long minutes) {
    return track.record(
        device,
        new TemperatureReading(
            new SensorEventId(UUID.randomUUID()),
            device.id(),
            fridge.id(),
            T0.plus(Duration.ofMinutes(minutes)),
            new BigDecimal(celsius)));
  }

  private List<InventoryEntry> inventory() {
    return new ViewInventory(
            households, fridges, stores.ownerships, chains, FreshnessPolicy.DEFAULT)
        .view(juan, household.id(), LocalDate.of(2026, 10, 3));
  }

  @Test
  void breachPutsPerishablesUnderReviewUntilAHumanReviews() {
    reading("4", 0);
    reading("8", 5);
    assertEquals(ColdChainPhase.UNDER_REVIEW, reading("10", 30));

    Map<String, InventoryEntry> byName =
        inventory().stream().collect(Collectors.toMap(e -> e.food().item().name(), e -> e));
    assertTrue(byName.get("Chicken breast").food().has(FoodStatus.UNDER_REVIEW));
    assertFalse(byName.get("Chicken breast").food().isEdible());
    assertFalse(byName.get("Rice").food().has(FoodStatus.UNDER_REVIEW));

    ReviewColdChain review = new ReviewColdChain(households, fridges, chains, clock);
    assertThrows(
        IllegalStateException.class, () -> review.review(juan, household.id(), fridge.id()));
    reading("4", 50);
    assertThrows(
        AccessDeniedException.class, () -> review.review(guest, household.id(), fridge.id()));

    ColdIncident incident = review.review(juan, household.id(), fridge.id());

    assertEquals(new BigDecimal("10"), incident.peakCelsius());
    assertEquals(
        List.of(ColdChainPhase.NORMAL),
        new ListColdChains(households, fridges, chains)
            .list(guest, household.id()).stream().map(chain -> chain.phase()).toList());
    assertTrue(inventory().stream().noneMatch(entry -> entry.food().has(FoodStatus.UNDER_REVIEW)));
  }

  @Test
  void reviewRequiresAFridgeOfTheHouseholdAndReadingsTheirDevice() {
    ReviewColdChain review = new ReviewColdChain(households, fridges, chains, clock);

    assertThrows(
        FridgeNotFoundException.class, () -> review.review(juan, household.id(), FridgeId.newId()));
    assertThrows(
        IllegalStateException.class, () -> review.review(juan, household.id(), fridge.id()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            track.record(
                device,
                new TemperatureReading(
                    new SensorEventId(UUID.randomUUID()),
                    device.id(),
                    FridgeId.newId(),
                    T0,
                    BigDecimal.ONE)));
  }
}
