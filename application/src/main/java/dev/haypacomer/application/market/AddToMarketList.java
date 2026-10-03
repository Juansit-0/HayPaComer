package dev.haypacomer.application.market;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.MarketItem;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Clock;
import java.util.Objects;

public final class AddToMarketList {

  private final GetHousehold households;
  private final MarketListRepository lists;
  private final FoodCatalogRepository catalog;
  private final Clock clock;

  public AddToMarketList(
      HouseholdRepository households,
      MarketListRepository lists,
      FoodCatalogRepository catalog,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.lists = Objects.requireNonNull(lists, "lists");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public MarketItem add(
      UserId actor, HouseholdId household, String foodName, Grams grams, MarketSource source) {
    households.get(actor, household).requirePermission(actor, Permission.MANAGE_MARKET_LIST);
    FoodMetadata food =
        catalog.findByName(foodName).orElseThrow(() -> new FoodNotInCatalogException(foodName));
    MarketList list = lists.findByHousehold(household).orElseGet(() -> MarketList.empty(household));
    MarketItem item = list.add(food, grams, source, actor, clock.instant());
    lists.save(list);
    return item;
  }
}
