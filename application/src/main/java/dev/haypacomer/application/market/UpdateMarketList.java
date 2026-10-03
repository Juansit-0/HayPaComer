package dev.haypacomer.application.market;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.MarketItemId;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class UpdateMarketList {

  private final GetHousehold households;
  private final MarketListRepository lists;
  private final Clock clock;

  public UpdateMarketList(HouseholdRepository households, MarketListRepository lists, Clock clock) {
    this.households = new GetHousehold(households);
    this.lists = Objects.requireNonNull(lists, "lists");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public MarketList update(UserId actor, HouseholdId household, Change change) {
    households.get(actor, household).requirePermission(actor, Permission.MANAGE_MARKET_LIST);
    MarketList list = lists.findByHousehold(household).orElseGet(() -> MarketList.empty(household));
    change.apply(list, clock.instant());
    lists.save(list);
    return list;
  }

  public sealed interface Change permits SetGrams, Check, Uncheck, Remove, ClearChecked {

    void apply(MarketList list, Instant now);
  }

  public record SetGrams(MarketItemId item, Grams grams) implements Change {

    @Override
    public void apply(MarketList list, Instant now) {
      list.changeGrams(item, grams);
    }
  }

  public record Check(MarketItemId item) implements Change {

    @Override
    public void apply(MarketList list, Instant now) {
      list.check(item, now);
    }
  }

  public record Uncheck(MarketItemId item) implements Change {

    @Override
    public void apply(MarketList list, Instant now) {
      list.uncheck(item);
    }
  }

  public record Remove(MarketItemId item) implements Change {

    @Override
    public void apply(MarketList list, Instant now) {
      list.remove(item);
    }
  }

  public record ClearChecked() implements Change {

    @Override
    public void apply(MarketList list, Instant now) {
      list.clearChecked();
    }
  }
}
