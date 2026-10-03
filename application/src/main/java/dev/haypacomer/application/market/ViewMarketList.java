package dev.haypacomer.application.market;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.MarketList;
import java.util.Objects;

public final class ViewMarketList {

  private final GetHousehold households;
  private final MarketListRepository lists;

  public ViewMarketList(HouseholdRepository households, MarketListRepository lists) {
    this.households = new GetHousehold(households);
    this.lists = Objects.requireNonNull(lists, "lists");
  }

  public MarketList view(UserId actor, HouseholdId household) {
    households.get(actor, household);
    return lists.findByHousehold(household).orElseGet(() -> MarketList.empty(household));
  }
}
