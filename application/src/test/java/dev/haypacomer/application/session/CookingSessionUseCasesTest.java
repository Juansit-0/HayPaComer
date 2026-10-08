package dev.haypacomer.application.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.support.InMemoryCookingSessionRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.AccessDeniedException;
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
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.IllegalSessionTransitionException;
import dev.haypacomer.domain.session.SessionPhase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CookingSessionUseCasesTest {

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
          List.of(RecipeStep.of(1, "Boil"), RecipeStep.of(2, "Serve")));

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryCookingSessionRepository sessions = new InMemoryCookingSessionRepository();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId guest = UserId.newId();
  private final UserId stranger = UserId.newId();
  private final StartCookingSession start = new StartCookingSession(households, sessions, clock);
  private final AdvanceCookingSession advance =
      new AdvanceCookingSession(households, sessions, clock);
  private final ViewCookingSession view = new ViewCookingSession(households, sessions);
  private final ResumeCookingSession resume = new ResumeCookingSession(households, sessions);
  private Household household;

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
  }

  @Test
  void cooksARecipeAndResumesTheActiveSession() {
    CookingSession session = start.start(juan, household.id(), RICE_BOWL, 4);

    assertEquals(Grams.of(300), session.recipe().requirements().getFirst().grams());
    assertEquals(session.id(), resume.active(guest, household.id()).orElseThrow().id());
    advance.apply(juan, household.id(), session.id(), SessionAction.NEXT);
    advance.apply(juan, household.id(), session.id(), SessionAction.PAUSE);
    assertEquals(SessionPhase.PAUSED, view.view(guest, household.id(), session.id()).phase());
    advance.apply(juan, household.id(), session.id(), SessionAction.RESUME);
    advance.apply(juan, household.id(), session.id(), SessionAction.NEXT);
    CookingSession finished = advance.apply(juan, household.id(), session.id(), SessionAction.NEXT);

    assertEquals(SessionPhase.FINISHED, finished.phase());
    assertTrue(resume.active(juan, household.id()).isEmpty());
    assertEquals(SessionPhase.PREPARING, start.start(juan, household.id(), RICE_BOWL, 2).phase());
  }

  @Test
  void onlyOneActiveSessionPerHousehold() {
    CookingSession first = start.start(juan, household.id(), RICE_BOWL, 2);

    SessionAlreadyActiveException conflict =
        assertThrows(
            SessionAlreadyActiveException.class,
            () -> start.start(juan, household.id(), RICE_BOWL, 2));
    assertEquals(first.id(), conflict.active());

    advance.apply(juan, household.id(), first.id(), SessionAction.ABANDON);
    start.start(juan, household.id(), RICE_BOWL, 2);
  }

  @Test
  void guestsWatchButDoNotCookAndStrangersSeeNothing() {
    CookingSession session = start.start(juan, household.id(), RICE_BOWL, 2);

    assertThrows(
        AccessDeniedException.class, () -> start.start(guest, household.id(), RICE_BOWL, 2));
    assertThrows(
        AccessDeniedException.class,
        () -> advance.apply(guest, household.id(), session.id(), SessionAction.NEXT));
    assertThrows(HouseholdNotFoundException.class, () -> resume.active(stranger, household.id()));
    assertThrows(
        HouseholdNotFoundException.class, () -> view.view(stranger, household.id(), session.id()));
  }

  @Test
  void rejectsUnknownSessionsAndIllegalTransitions() {
    CookingSession session = start.start(juan, household.id(), RICE_BOWL, 2);
    CookingSessionId unknown = CookingSessionId.newId();

    assertThrows(
        CookingSessionNotFoundException.class, () -> view.view(juan, household.id(), unknown));
    assertThrows(
        CookingSessionNotFoundException.class,
        () -> advance.apply(juan, household.id(), unknown, SessionAction.NEXT));
    assertThrows(
        IllegalSessionTransitionException.class,
        () -> advance.apply(juan, household.id(), session.id(), SessionAction.PAUSE));
  }
}
