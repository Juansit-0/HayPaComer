package dev.haypacomer.application.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.market.UpdateMarketList.Check;
import dev.haypacomer.application.market.UpdateMarketList.ClearChecked;
import dev.haypacomer.application.market.UpdateMarketList.Remove;
import dev.haypacomer.application.market.UpdateMarketList.SetGrams;
import dev.haypacomer.application.market.UpdateMarketList.Uncheck;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.MarketItem;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketListUseCasesTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final Map<HouseholdId, MarketList> saved = new HashMap<>();
  private final MarketListRepository lists =
      new MarketListRepository() {
        @Override
        public void save(MarketList list) {
          saved.put(list.household(), MarketList.restore(list.household(), list.items()));
        }

        @Override
        public Optional<MarketList> findByHousehold(HouseholdId household) {
          return Optional.ofNullable(saved.get(household))
              .map(list -> MarketList.restore(household, list.items()));
        }
      };
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final UserId guest = UserId.newId();
  private Household household;

  @BeforeEach
  void createHousehold() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
    stores.catalog.save(
        new FoodMetadata(
            "Rice",
            FoodCategory.GRAIN,
            Unit.GRAM,
            ConversionFactors.MASS_ONLY,
            false,
            365,
            Set.of()));
  }

  private AddToMarketList add() {
    return new AddToMarketList(households, lists, stores.catalog, clock);
  }

  private UpdateMarketList update() {
    return new UpdateMarketList(households, lists, clock);
  }

  private MarketList view(UserId actor) {
    return new ViewMarketList(households, lists).view(actor, household.id());
  }

  @Test
  void membersShareOneListWithoutDuplicates() {
    MarketItem first = add().add(juan, household.id(), "Rice", Grams.of(500), MarketSource.MANUAL);
    MarketItem merged = add().add(ana, household.id(), "rice", Grams.of(500), MarketSource.RECIPE);

    assertEquals(first.id(), merged.id());
    assertEquals(1, view(guest).pending().size());
    assertEquals(Grams.of(1000), view(guest).pending().getFirst().grams());
  }

  @Test
  void guestsAndOutsidersCannotChangeTheList() {
    assertThrows(
        AccessDeniedException.class,
        () -> add().add(guest, household.id(), "Rice", Grams.of(1), MarketSource.MANUAL));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> new ViewMarketList(households, lists).view(UserId.newId(), household.id()));
    assertThrows(
        FoodNotInCatalogException.class,
        () -> add().add(juan, household.id(), "Dragon fruit", Grams.of(1), MarketSource.MANUAL));
    assertTrue(view(juan).items().isEmpty());
  }

  @Test
  void appliesChangesAndPersistsThem() {
    MarketItem rice = add().add(juan, household.id(), "Rice", Grams.of(500), MarketSource.MANUAL);

    update().update(ana, household.id(), new SetGrams(rice.id(), Grams.of(700)));
    update().update(ana, household.id(), new Check(rice.id()));
    assertTrue(view(juan).pending().isEmpty());
    update().update(ana, household.id(), new Uncheck(rice.id()));
    assertEquals(Grams.of(700), view(juan).pending().getFirst().grams());
    update().update(juan, household.id(), new Check(rice.id()));
    update().update(juan, household.id(), new ClearChecked());
    assertTrue(view(juan).items().isEmpty());

    MarketItem again = add().add(juan, household.id(), "Rice", Grams.of(100), MarketSource.MANUAL);
    update().update(juan, household.id(), new Remove(again.id()));
    assertTrue(view(juan).items().isEmpty());
    assertThrows(
        AccessDeniedException.class,
        () -> update().update(guest, household.id(), new ClearChecked()));
  }
}
