package dev.haypacomer.domain.coldchain.investigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ColdInvestigatorTest {

  private static final Instant T0 = Instant.parse("2026-10-09T08:00:00Z");
  private static final FridgeId FRIDGE = FridgeId.newId();
  private static final DeviceId DEVICE = DeviceId.newId();

  private final ColdInvestigator investigator = new ColdInvestigator(FridgeThresholds.DEFAULT);

  private static TemperatureReading temp(int minute, String celsius) {
    return new TemperatureReading(
        new SensorEventId(UUID.randomUUID()),
        DEVICE,
        FRIDGE,
        T0.plus(Duration.ofMinutes(minute)),
        new BigDecimal(celsius));
  }

  private static DoorEvent door(int minute, DoorState state) {
    return new DoorEvent(
        new SensorEventId(UUID.randomUUID()),
        DEVICE,
        FRIDGE,
        T0.plus(Duration.ofMinutes(minute)),
        state);
  }

  private static FoodItem item(String name, boolean perishable) {
    return new FoodItem(
        FoodItemId.newId(),
        new FoodMetadata(
            name,
            FoodCategory.OTHER,
            Unit.GRAM,
            ConversionFactors.MASS_ONLY,
            perishable,
            5,
            Set.of()),
        Grams.of(500),
        Grams.ZERO,
        null);
  }

  @Test
  void aDoorLeftOpenWarmsTheFridgeAndPerishablesMustBeUsedToday() {
    List<SensorEvent> events =
        List.of(
            temp(0, "4.0"),
            door(5, DoorState.OPEN),
            temp(10, "6.5"),
            temp(20, "8.1"),
            door(25, DoorState.CLOSED),
            temp(30, "7.0"),
            temp(50, "4.5"));

    ColdInvestigation result =
        investigator.investigate(
            FRIDGE,
            T0,
            T0.plus(Duration.ofHours(2)),
            events,
            List.of(item("Chicken breast", true), item("Rice", false)));

    ColdEpisode episode = result.episodes().getFirst();
    assertEquals(LikelyCause.DOOR_LEFT_OPEN, episode.cause());
    assertEquals(T0.plus(Duration.ofMinutes(10)), episode.start());
    assertEquals(T0.plus(Duration.ofMinutes(50)), episode.ended().orElseThrow());
    assertEquals(new BigDecimal("8.1"), episode.peakCelsius());
    assertEquals(Duration.ofMinutes(40), episode.aboveLimit());
    assertEquals(Duration.ofMinutes(20), episode.doorOpen());
    assertEquals(5, result.readings());
    assertEquals(FoodVerdict.USE_TODAY, result.foods().getFirst().verdict());
    assertEquals(FoodVerdict.KEEP, result.foods().get(1).verdict());
    assertEquals("Not perishable", result.foods().get(1).reason());
  }

  @Test
  void anOngoingWarmFridgeWithTheDoorShutPointsAtCoolingAndDiscardsPerishables() {
    List<SensorEvent> events = new ArrayList<>();
    events.add(temp(0, "4.0"));
    for (int minute = 10; minute <= 120; minute += 10) {
      events.add(temp(minute, "7.5"));
    }

    ColdInvestigation result =
        investigator.investigate(
            FRIDGE, T0, T0.plus(Duration.ofMinutes(130)), events, List.of(item("Milk", true)));

    ColdEpisode episode = result.episodes().getFirst();
    assertEquals(LikelyCause.COOLING_OR_POWER, episode.cause());
    assertTrue(episode.ended().isEmpty());
    assertEquals(Duration.ofMinutes(120), result.totalAboveLimit());
    assertEquals(FoodVerdict.DISCARD, result.foods().getFirst().verdict());
    assertTrue(result.foods().getFirst().reason().contains("120 min"));
  }

  @Test
  void silenceBetweenReadingsIsReportedAsMissingReadings() {
    List<SensorEvent> events = List.of(temp(0, "6.0"), temp(40, "6.2"), temp(45, "4.8"));

    ColdInvestigation result =
        investigator.investigate(
            FRIDGE, T0, T0.plus(Duration.ofHours(1)), events, List.of(item("Ham", true)));

    assertEquals(LikelyCause.READINGS_MISSING, result.episodes().getFirst().cause());
    assertEquals(FoodVerdict.USE_TODAY, result.foods().getFirst().verdict());
  }

  @Test
  void aColdFridgeKeepsEverythingAndBadWindowsAreRejected() {
    ColdInvestigation result =
        investigator.investigate(
            FRIDGE,
            T0,
            T0.plus(Duration.ofHours(1)),
            List.of(temp(0, "3.9"), temp(30, "4.1"), door(10, DoorState.OPEN)),
            List.of(item("Yogurt", true)));

    assertTrue(result.episodes().isEmpty());
    assertEquals(FoodVerdict.KEEP, result.foods().getFirst().verdict());
    assertEquals("No time above 5 C", result.foods().getFirst().reason());
    assertThrows(
        IllegalArgumentException.class,
        () -> investigator.investigate(FRIDGE, T0, T0, List.of(), List.of()));
  }

  @Test
  void briefWarmthIsKept() {
    ColdInvestigation result =
        investigator.investigate(
            FRIDGE,
            T0,
            T0.plus(Duration.ofHours(1)),
            List.of(temp(0, "5.5"), temp(10, "4.0")),
            List.of(item("Cheese", true)));

    assertEquals("Only 10 min above 5 C", result.foods().getFirst().reason());
  }
}
