package dev.haypacomer.agent.kitchen;

import dev.haypacomer.application.coldchain.ListColdChains;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.market.AddToMarketList;
import dev.haypacomer.application.market.ViewMarketList;
import dev.haypacomer.application.planning.ViewCurrentPlan;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.application.support.InMemoryPlanningStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class KitchenFixture {

  public static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");
  public static final LocalDate TODAY = LocalDate.parse("2026-10-09");

  public final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  public final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  public final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  public final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  public final InMemoryPlanningStores planning = new InMemoryPlanningStores();
  public final InMemoryColdChainRepository chains = new InMemoryColdChainRepository();
  public final Map<HouseholdId, MarketList> lists = new HashMap<>();
  public final MarketListRepository marketLists =
      new MarketListRepository() {
        @Override
        public void save(MarketList list) {
          lists.put(list.household(), list);
        }

        @Override
        public Optional<MarketList> findByHousehold(HouseholdId household) {
          return Optional.ofNullable(lists.get(household));
        }
      };
  public final UserId owner = UserId.newId();
  public final UserId guest = UserId.newId();
  public final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneOffset.UTC, owner, NOW);
  public final Fridge fridge;
  public final KitchenToday today = new KitchenToday(households, clock);
  public final ViewInventory inventory;

  public KitchenFixture() {
    home.join(guest, Role.GUEST, NOW);
    households.save(home);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(owner, home.id(), "Kitchen", FridgeLayout.STANDARD);
    inventory =
        new ViewInventory(households, fridges, stores.ownerships, chains, FreshnessPolicy.DEFAULT);
  }

  public static FoodMetadata food(String name) {
    return new FoodMetadata(
        name, FoodCategory.OTHER, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  public FoodItem put(String name, long grams, LocalDate expiry) {
    FoodMetadata food = food(name);
    stores.catalog.save(food);
    FoodItem item = new FoodItem(FoodItemId.newId(), food, Grams.of(grams), Grams.ZERO, expiry);
    fridge.place(item, fridge.trays().findFirst().orElseThrow().id());
    return item;
  }

  public ViewMarketList viewMarket() {
    return new ViewMarketList(households, marketLists);
  }

  public AddToMarketList addToMarket() {
    return new AddToMarketList(households, marketLists, stores.catalog, clock);
  }

  public ListColdChains coldChains() {
    return new ListColdChains(households, fridges, chains);
  }

  public ViewCurrentPlan currentPlan() {
    return new ViewCurrentPlan(households, planning.plans, clock);
  }
}
