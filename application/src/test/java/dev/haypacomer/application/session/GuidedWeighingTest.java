package dev.haypacomer.application.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.scale.NoRecentSampleException;
import dev.haypacomer.application.scale.ReadWeighingProgress;
import dev.haypacomer.application.support.InMemoryCookingSessionRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryKitchenDevices;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
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
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.scale.ScaleCalibration;
import dev.haypacomer.domain.scale.WeighingStatus;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.IllegalSessionTransitionException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuidedWeighingTest {

  private static final Instant NOW = Instant.parse("2026-10-08T18:00:00Z");
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
              RecipeStep.of(2, "Boil").withTimer(Duration.ofMinutes(15))));

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryCookingSessionRepository sessions = new InMemoryCookingSessionRepository();
  private final InMemoryKitchenDevices kitchen = new InMemoryKitchenDevices();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final KitchenMediator mediator =
      new GuidedCookingMediator(kitchen.scales, kitchen.timers, kitchen.devices, kitchen.hardware);
  private final UserId juan = UserId.newId();
  private final UserId guest = UserId.newId();
  private final WeighStep weigh =
      new WeighStep(
          households,
          sessions,
          new ReadWeighingProgress(
              households,
              kitchen.devices,
              kitchen.samples,
              kitchen.calibrations,
              kitchen.scales,
              clock),
          mediator,
          clock);
  private final ViewStepTimer timer =
      new ViewStepTimer(
          households, sessions, kitchen.timers, Clock.offset(clock, Duration.ofMinutes(4)));
  private Household household;
  private Device scale;
  private CookingSession session;

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
    scale =
        Device.register(
            household.id(), FridgeId.newId(), "Scale", DeviceKind.ESP32_SCALE, "ab", NOW);
    kitchen.devices.save(scale);
    kitchen.calibrations.save(
        scale.id(),
        ScaleCalibration.taredAt(new RawSample(0, NOW))
            .calibrate(new RawSample(1_000, NOW), Grams.of(1)));
    session =
        new StartCookingSession(households, sessions, kitchen.devices, mediator, clock)
            .start(juan, household.id(), RICE_BOWL, 2, Optional.of(scale.id()));
  }

  private void next() {
    new AdvanceCookingSession(households, sessions, mediator, clock)
        .apply(juan, household.id(), session.id(), SessionAction.NEXT);
  }

  @Test
  void readsTheSessionScaleAndBlinksOnTarget() {
    next();
    kitchen.samples.record(scale.id(), new RawSample(120_000, NOW));

    StepWeighingResult shortOf =
        weigh.weigh(juan, household.id(), session.id(), 1, Optional.empty());
    assertEquals(WeighingStatus.SHORT, shortOf.progress().status());
    assertEquals(Grams.of(30), shortOf.progress().remaining());
    assertTrue(kitchen.signals.isEmpty());

    kitchen.samples.record(scale.id(), new RawSample(151_000, NOW));
    StepWeighingResult done = weigh.weigh(juan, household.id(), session.id(), 1, Optional.empty());

    assertEquals(WeighingStatus.ON_TARGET, done.progress().status());
    assertEquals(List.of("Scale:WEIGHT_CONFIRMED_BLINK"), kitchen.signals);
    next();
    assertEquals(
        Grams.of(151),
        sessions
            .find(household.id(), session.id())
            .orElseThrow()
            .completions()
            .getFirst()
            .measuredGrams()
            .orElseThrow());
  }

  @Test
  void acceptsManualGramsAndReportsTheTimer() {
    assertTrue(timer.view(guest, household.id(), session.id()).isEmpty());
    next();

    assertEquals(
        WeighingStatus.OVER,
        weigh
            .weigh(juan, household.id(), session.id(), 1, Optional.of(Grams.of(200)))
            .progress()
            .status());
    next();

    TimerStatus status = timer.view(guest, household.id(), session.id()).orElseThrow();
    assertEquals(2, status.step());
    assertEquals(Duration.ofMinutes(11), status.remaining());
    assertFalse(status.paused());
    assertFalse(status.done());
  }

  @Test
  void refusesTheWrongStepAndMissingReadings() {
    assertThrows(
        IllegalSessionTransitionException.class,
        () -> weigh.weigh(juan, household.id(), session.id(), 1, Optional.of(Grams.of(1))));
    next();
    assertThrows(
        NoRecentSampleException.class,
        () -> weigh.weigh(juan, household.id(), session.id(), 1, Optional.empty()));
    assertThrows(
        CookingSessionNotFoundException.class,
        () ->
            weigh.weigh(
                juan, household.id(), CookingSessionId.newId(), 1, Optional.of(Grams.of(1))));
    next();
    assertThrows(
        IllegalArgumentException.class,
        () -> weigh.weigh(juan, household.id(), session.id(), 2, Optional.of(Grams.of(1))));
  }

  @Test
  void withoutAScaleTheCookTypesTheGrams() {
    CookingSession manual =
        new StartCookingSession(
                households,
                new InMemoryCookingSessionRepository(),
                kitchen.devices,
                mediator,
                clock)
            .start(juan, household.id(), RICE_BOWL, 2, Optional.empty());
    InMemoryCookingSessionRepository other = new InMemoryCookingSessionRepository();
    manual.next(NOW);
    other.save(manual);
    WeighStep weighManual =
        new WeighStep(
            households,
            other,
            new ReadWeighingProgress(
                households,
                kitchen.devices,
                kitchen.samples,
                kitchen.calibrations,
                kitchen.scales,
                clock),
            mediator,
            clock);

    assertThrows(
        IllegalArgumentException.class,
        () -> weighManual.weigh(juan, household.id(), manual.id(), 1, Optional.empty()));
    assertEquals(
        WeighingStatus.ON_TARGET,
        weighManual
            .weigh(juan, household.id(), manual.id(), 1, Optional.of(Grams.of(150)))
            .progress()
            .status());
  }
}
