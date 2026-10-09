package dev.haypacomer.agent.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.application.live.BroadcastLiveUpdate;
import dev.haypacomer.application.live.LiveUpdate;
import dev.haypacomer.application.live.LiveUpdateKind;
import dev.haypacomer.application.sensor.WeightReadingHandler;
import dev.haypacomer.application.sensor.WeightReadingHandlers;
import dev.haypacomer.application.support.InMemoryCookingSessionRepository;
import dev.haypacomer.application.support.InMemoryKitchenDevices;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.WeightReading;
import dev.haypacomer.domain.session.CookingSession;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScaleCopilotTest {

  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");
  private static final FoodMetadata RICE = food("Rice");
  private static final FoodMetadata CHICKEN = food("Chicken breast");

  private final HouseholdId home = HouseholdId.newId();
  private final Device scale =
      Device.register(home, FridgeId.newId(), "Counter scale", DeviceKind.ESP32_SCALE, "hash", NOW);
  private final InMemoryKitchenDevices devices = new InMemoryKitchenDevices();
  private final InMemoryCookingSessionRepository sessions = new InMemoryCookingSessionRepository();
  private final List<LiveUpdate> published = new ArrayList<>();
  private final ScaleCopilot copilot =
      new ScaleCopilot(devices.scales, sessions, new BroadcastLiveUpdate(List.of(published::add)));

  private static FoodMetadata food(String name) {
    return new FoodMetadata(
        name, FoodCategory.OTHER, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private WeightReading reading(long grams, boolean stable, ScaleMode mode) {
    return new WeightReading(
        new SensorEventId(UUID.randomUUID()),
        scale.id(),
        scale.fridge(),
        NOW,
        Grams.of(grams),
        stable,
        mode,
        null);
  }

  private List<String> hints() {
    return published.stream().map(LiveUpdate::detail).toList();
  }

  @Test
  void guidesThePourAndOnlySpeaksWhenSomethingChanges() {
    devices.scales.cook(scale.id(), WeighingTarget.of("Rice", Grams.of(150)));

    copilot.apply(scale, reading(60, true, ScaleMode.COOKING));
    copilot.apply(scale, reading(62, true, ScaleMode.COOKING));
    copilot.apply(scale, reading(120, true, ScaleMode.COOKING));
    copilot.apply(scale, reading(150, true, ScaleMode.COOKING));
    copilot.apply(scale, reading(151, true, ScaleMode.COOKING));
    copilot.apply(scale, reading(170, false, ScaleMode.COOKING));

    assertEquals(
        List.of(
            "Add 90 g more Rice (40%).",
            "Add 30 g more Rice (80%).", "Rice is on target at 150 g. Confirm the step to go on."),
        hints());
    assertEquals(LiveUpdateKind.COPILOT, published.getFirst().kind());
    assertEquals(home, published.getFirst().household());
  }

  @Test
  void overPouringOffersToScaleTheRestOfTheRecipe() {
    Recipe recipe =
        new Recipe(
            RecipeId.newId(),
            "Rice with chicken",
            2,
            30,
            RecipeSource.MANUAL,
            List.of(
                RecipeRequirement.of(RICE, Grams.of(150)),
                RecipeRequirement.of(CHICKEN, Grams.of(200))),
            List.of());
    CookingSession session = CookingSession.start(home, recipe, 2, UserId.newId(), NOW);
    session.useScale(scale.id());
    sessions.save(session);
    devices.scales.cook(scale.id(), WeighingTarget.of("Rice", Grams.of(150)));

    copilot.apply(scale, reading(180, true, ScaleMode.COOKING));

    assertEquals(
        "30 g over on Rice (180 g of 150 g). Take 30 g out, or keep it and scale the rest by 1.20:"
            + " Chicken breast 240 g instead of 200 g.",
        hints().getFirst());
  }

  @Test
  void staysQuietOutsideGuidedCooking() {
    copilot.apply(scale, reading(100, true, ScaleMode.COOKING));
    copilot.apply(scale, reading(100, true, ScaleMode.FRIDGE));
    devices.scales.cook(scale.id(), WeighingTarget.of("Rice", Grams.of(100)));
    copilot.apply(scale, reading(300, true, ScaleMode.COOKING));

    assertEquals(List.of("200 g over on Rice (300 g of 100 g). Take 200 g out."), hints());
  }

  @Test
  void handlersAllRunAndTheFirstOutcomeWins() {
    CommandOutcome outcome =
        new CommandOutcome(UUID.randomUUID(), FoodItemId.newId(), Grams.of(10), false);
    List<String> order = new ArrayList<>();
    WeightReadingHandler first =
        (device, reading) -> {
          order.add("first");
          return Optional.empty();
        };
    WeightReadingHandler second =
        (device, reading) -> {
          order.add("second");
          return Optional.of(outcome);
        };
    WeightReadingHandler third =
        (device, reading) -> {
          order.add("third");
          return Optional.empty();
        };

    assertEquals(
        Optional.of(outcome),
        new WeightReadingHandlers(List.of(first, second, third))
            .apply(scale, reading(1, true, ScaleMode.FRIDGE)));
    assertEquals(List.of("first", "second", "third"), order);
    assertTrue(published.isEmpty());
  }
}
