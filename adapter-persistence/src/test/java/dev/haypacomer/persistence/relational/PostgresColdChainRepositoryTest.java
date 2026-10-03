package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.coldchain.ColdChainPhase;
import dev.haypacomer.domain.coldchain.UnderReview;
import dev.haypacomer.domain.coldchain.Warming;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class PostgresColdChainRepositoryTest extends PostgresTestSupport {

  @Test
  void persistsEveryStateAndTheReviewedIncident() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    Fridge fridge = Fridge.named("Kitchen");
    new PostgresFridgeRepository(dataSource).save(household.id(), fridge);
    PostgresColdChainRepository chains = new PostgresColdChainRepository(dataSource);

    assertTrue(chains.find(fridge.id()).isEmpty());
    ColdChain chain = ColdChain.start(fridge.id());
    chain.record(new BigDecimal("8"), NOW, FridgeThresholds.DEFAULT);
    chains.save(chain);
    assertInstanceOf(Warming.class, chains.find(fridge.id()).orElseThrow().state());

    ColdChain loaded = chains.find(fridge.id()).orElseThrow();
    loaded.record(
        new BigDecimal("11.25"), NOW.plus(Duration.ofMinutes(25)), FridgeThresholds.DEFAULT);
    loaded.record(new BigDecimal("4"), NOW.plus(Duration.ofMinutes(40)), FridgeThresholds.DEFAULT);
    chains.save(loaded);
    UnderReview review =
        assertInstanceOf(UnderReview.class, chains.find(fridge.id()).orElseThrow().state());
    assertEquals(0, new BigDecimal("11.25").compareTo(review.highest()));
    assertTrue(review.recovered());

    ColdChain again = chains.find(fridge.id()).orElseThrow();
    again.review(juan.id(), NOW.plus(Duration.ofMinutes(45)));
    chains.save(again);
    chains.save(again);

    assertEquals(ColdChainPhase.NORMAL, chains.find(fridge.id()).orElseThrow().phase());
    assertEquals(
        1,
        JdbcClient.create(dataSource)
            .sql("SELECT count(*) FROM cold_incidents")
            .query(Integer.class)
            .single());
    assertTrue(chains.find(FridgeId.newId()).isEmpty());
  }
}
