package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.CHICKEN;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.EGG;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.Paused;
import dev.haypacomer.domain.session.SessionPhase;
import dev.haypacomer.domain.session.StepCompletion;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostgresCookingSessionRepositoryTest extends PostgresTestSupport {

  private static final Recipe OMELETTE =
      new Recipe(
          RecipeId.newId(),
          "Chicken omelette",
          1,
          15,
          RecipeSource.MANUAL,
          List.of(
              RecipeRequirement.of(EGG, Grams.of(100)),
              RecipeRequirement.optional(CHICKEN, Grams.of(50))),
          List.of(
              RecipeStep.of(1, "Weigh the chicken")
                  .withWeighing(new StepWeighing(CHICKEN, Grams.of(50))),
              RecipeStep.of(2, "Cook").withTimer(Duration.ofMinutes(4)),
              RecipeStep.of(3, "Serve")));

  @Test
  void storesTheRecipeStateAndCompletedSteps() {
    PostgresFoodCatalogRepository catalog = new PostgresFoodCatalogRepository(dataSource);
    catalog.save(EGG);
    catalog.save(CHICKEN);
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    PostgresCookingSessionRepository sessions = new PostgresCookingSessionRepository(dataSource);

    CookingSession session = CookingSession.start(household.id(), OMELETTE, 2, juan.id(), NOW);
    sessions.save(session);
    assertEquals(session.id(), sessions.active(household.id()).orElseThrow().id());

    session.next(NOW.plusSeconds(10));
    session.next(NOW.plusSeconds(70));
    session.pause(NOW.plusSeconds(80));
    sessions.save(session);

    CookingSession loaded = sessions.find(household.id(), session.id()).orElseThrow();
    assertEquals(new Paused(2, NOW.plusSeconds(80)), loaded.state());
    assertEquals(List.of(new StepCompletion(1, NOW.plusSeconds(70))), loaded.completions());
    assertEquals(session.recipe(), loaded.recipe());
    assertEquals(
        Grams.of(100), loaded.recipe().steps().getFirst().weighingTarget().orElseThrow().target());
    assertEquals(Duration.ofMinutes(4), loaded.step().orElseThrow().timerDuration().orElseThrow());
    assertEquals(juan.id(), loaded.startedBy());

    loaded.resume(NOW.plusSeconds(90));
    loaded.next(NOW.plusSeconds(300));
    loaded.next(NOW.plusSeconds(360));
    sessions.save(loaded);

    CookingSession finished = sessions.find(household.id(), session.id()).orElseThrow();
    assertEquals(SessionPhase.FINISHED, finished.phase());
    assertEquals(3, finished.completions().size());
    assertEquals(NOW.plusSeconds(360), finished.updatedAt());
    assertTrue(sessions.active(household.id()).isEmpty());
    assertTrue(sessions.find(household.id(), CookingSessionId.newId()).isEmpty());
  }
}
