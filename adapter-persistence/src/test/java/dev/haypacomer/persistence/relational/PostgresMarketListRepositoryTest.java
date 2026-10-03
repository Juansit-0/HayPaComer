package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.CHICKEN;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.market.MarketItem;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.Grams;
import java.time.ZoneId;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class PostgresMarketListRepositoryTest extends PostgresTestSupport {

  @Test
  void roundTripsTheListWithCheckedAndPendingItems() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    PostgresFoodCatalogRepository catalog = new PostgresFoodCatalogRepository(dataSource);
    catalog.save(MILK);
    catalog.save(CHICKEN);
    PostgresMarketListRepository lists = new PostgresMarketListRepository(dataSource);

    assertTrue(lists.findByHousehold(household.id()).isEmpty());

    MarketList list = MarketList.empty(household.id());
    MarketItem milk = list.add(MILK, Grams.of(1000), MarketSource.MANUAL, juan.id(), NOW);
    list.check(milk.id(), NOW.plusSeconds(60));
    list.add(MILK, Grams.of(500), MarketSource.RECIPE, juan.id(), NOW);
    list.add(CHICKEN, Grams.of(400), MarketSource.PLAN, juan.id(), NOW);
    lists.save(list);

    MarketList loaded = lists.findByHousehold(household.id()).orElseThrow();
    assertEquals(list.items(), loaded.items());

    loaded.clearChecked();
    lists.save(loaded);
    assertEquals(2, lists.findByHousehold(household.id()).orElseThrow().items().size());
    assertTrue(lists.findByHousehold(HouseholdId.newId()).isEmpty());
  }
}
