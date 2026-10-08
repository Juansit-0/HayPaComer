package dev.haypacomer.application.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.device.DeviceNotFoundException;
import dev.haypacomer.application.support.InMemoryCookingSessionRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryKitchenDevices;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.session.CookingSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuidedCookingMediatorTest {

  private static final Instant T0 = Instant.parse("2026-10-08T18:00:00Z");
  private static final FoodMetadata RICE =
      new FoodMetadata(
          "Rice", FoodCategory.GRAIN, Unit.GRAM, ConversionFactors.MASS_ONLY, false, 365, Set.of());
  private static final Recipe RICE_BOWL =
      new Recipe(
          RecipeId.newId(),
          "Rice bowl",
          2,
          25,
          RecipeSource.MANUAL,
          List.of(RecipeRequirement.of(RICE, Grams.of(150))),
          List.of(
              RecipeStep.of(1, "Weigh the rice")
                  .withWeighing(new StepWeighing(RICE, Grams.of(150))),
              RecipeStep.of(2, "Boil").withTimer(Duration.ofMinutes(15)),
              RecipeStep.of(3, "Rest").withTimer(Duration.ofMinutes(5)),
              RecipeStep.of(4, "Serve")));

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryCookingSessionRepository sessions = new InMemoryCookingSessionRepository();
  private final InMemoryKitchenDevices kitchen = new InMemoryKitchenDevices();
  private final AtomicReference<Instant> now = new AtomicReference<>(T0);
  private final Clock clock =
      new Clock() {
        @Override
        public ZoneId getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return now.get();
        }
      };
  private final GuidedCookingMediator mediator =
      new GuidedCookingMediator(kitchen.scales, kitchen.timers, kitchen.devices, kitchen.hardware);
  private final StartCookingSession start =
      new StartCookingSession(households, sessions, kitchen.devices, mediator, clock);
  private final AdvanceCookingSession advance =
      new AdvanceCookingSession(households, sessions, mediator, clock);
  private final CheckCookingTimers timers = new CheckCookingTimers(mediator, clock);
  private final UserId juan = UserId.newId();
  private Household household;
  private Device scale;

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, T0);
    households.save(household);
    scale =
        Device.register(
            household.id(), FridgeId.newId(), "Scale", DeviceKind.ESP32_SCALE, "ab", T0);
    kitchen.devices.save(scale);
  }

  private CookingSession act(CookingSession session, SessionAction action, long secondsLater) {
    now.set(now.get().plusSeconds(secondsLater));
    return advance.apply(juan, household.id(), session.id(), action);
  }

  @Test
  void coordinatesTheScaleAndTheTimersAlongTheRecipe() {
    CookingSession session =
        start.start(juan, household.id(), RICE_BOWL, 4, Optional.of(scale.id()));
    assertTrue(kitchen.cookingTargets.isEmpty());

    act(session, SessionAction.NEXT, 10);
    assertEquals(WeighingTarget.of("Rice", Grams.of(300)), kitchen.cookingTargets.get(scale.id()));
    assertTrue(kitchen.timerMap.isEmpty());

    act(session, SessionAction.NEXT, 20);
    assertEquals(2, kitchen.timerMap.get(session.id()).step());
    act(session, SessionAction.PAUSE, 300);
    act(session, SessionAction.RESUME, 3_600);
    now.set(now.get().plusSeconds(599));
    timers.check();
    assertTrue(kitchen.signals.isEmpty());
    now.set(now.get().plusSeconds(1));
    timers.check();
    timers.check();
    assertEquals(List.of("Scale:TIMER_DONE_BEEP"), kitchen.signals);

    act(session, SessionAction.NEXT, 5);
    assertEquals(3, kitchen.timerMap.get(session.id()).step());
    assertFalse(kitchen.timerMap.get(session.id()).signaled());
    act(session, SessionAction.NEXT, 5);
    assertTrue(kitchen.timerMap.isEmpty());

    act(session, SessionAction.NEXT, 5);
    assertTrue(kitchen.cookingTargets.isEmpty());
  }

  @Test
  void abandoningClearsTheTimerAndFreesTheScale() {
    CookingSession session =
        start.start(juan, household.id(), RICE_BOWL, 2, Optional.of(scale.id()));
    act(session, SessionAction.NEXT, 1);
    act(session, SessionAction.NEXT, 1);

    act(session, SessionAction.ABANDON, 1);
    now.set(now.get().plusSeconds(3_600));
    timers.check();

    assertTrue(kitchen.timerMap.isEmpty());
    assertTrue(kitchen.cookingTargets.isEmpty());
    assertTrue(kitchen.signals.isEmpty());
  }

  @Test
  void timersWorkWithoutAScale() {
    CookingSession session = start.start(juan, household.id(), RICE_BOWL, 2, Optional.empty());
    act(session, SessionAction.NEXT, 1);
    act(session, SessionAction.NEXT, 1);
    now.set(now.get().plusSeconds(900));

    timers.check();

    assertTrue(kitchen.timerMap.get(session.id()).signaled());
    assertTrue(kitchen.signals.isEmpty());
    assertTrue(kitchen.cookingTargets.isEmpty());
  }

  @Test
  void onlyAnActiveScaleOfTheHouseholdCanGuideTheSession() {
    Device door =
        Device.register(
            household.id(), FridgeId.newId(), "Door", DeviceKind.ESP32_DOOR_TEMP, "cd", T0);
    Device foreign =
        Device.register(
            HouseholdId.newId(), FridgeId.newId(), "Other", DeviceKind.ESP32_SCALE, "ef", T0);
    kitchen.devices.save(door);
    kitchen.devices.save(foreign);
    kitchen.devices.save(scale.revoke(T0));

    for (DeviceId device : List.of(door.id(), foreign.id(), scale.id(), DeviceId.newId())) {
      assertThrows(
          DeviceNotFoundException.class,
          () -> start.start(juan, household.id(), RICE_BOWL, 2, Optional.of(device)));
    }
    assertTrue(sessions.active(household.id()).isEmpty());
  }
}
