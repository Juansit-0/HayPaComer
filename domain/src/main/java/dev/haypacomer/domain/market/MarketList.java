package dev.haypacomer.domain.market;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MarketList {

  private final HouseholdId household;
  private final Map<MarketItemId, MarketItem> items = new LinkedHashMap<>();

  private MarketList(HouseholdId household) {
    this.household = Objects.requireNonNull(household, "household");
  }

  public static MarketList empty(HouseholdId household) {
    return new MarketList(household);
  }

  public static MarketList restore(HouseholdId household, List<MarketItem> items) {
    MarketList list = new MarketList(household);
    items.forEach(item -> list.items.put(item.id(), item));
    return list;
  }

  public HouseholdId household() {
    return household;
  }

  public List<MarketItem> items() {
    return List.copyOf(items.values());
  }

  public List<MarketItem> pending() {
    return items.values().stream().filter(MarketItem::isPending).toList();
  }

  public Map<FoodCategory, List<MarketItem>> pendingByCategory() {
    Map<FoodCategory, List<MarketItem>> grouped = new EnumMap<>(FoodCategory.class);
    pending().stream()
        .sorted(Comparator.comparing(item -> item.food().key()))
        .forEach(
            item ->
                grouped
                    .computeIfAbsent(item.food().category(), category -> new ArrayList<>())
                    .add(item));
    grouped.replaceAll((category, list) -> List.copyOf(list));
    return grouped;
  }

  public MarketItem add(
      FoodMetadata food, Grams grams, MarketSource source, UserId addedBy, Instant at) {
    Optional<MarketItem> existing = pendingFor(food);
    MarketItem item =
        existing
            .map(found -> found.withGrams(found.grams().plus(grams)))
            .orElseGet(
                () -> new MarketItem(MarketItemId.newId(), food, grams, source, addedBy, at, null));
    items.put(item.id(), item);
    return item;
  }

  public MarketItem changeGrams(MarketItemId id, Grams grams) {
    return replace(require(id).withGrams(grams));
  }

  public MarketItem check(MarketItemId id, Instant at) {
    MarketItem item = require(id);
    return item.isPending() ? replace(item.checked(at)) : item;
  }

  public MarketItem uncheck(MarketItemId id) {
    MarketItem item = require(id);
    if (item.isPending()) {
      return item;
    }
    if (pendingFor(item.food()).isPresent()) {
      throw new IllegalStateException(item.food().name() + " is already pending on the list");
    }
    return replace(item.unchecked());
  }

  public void remove(MarketItemId id) {
    items.remove(require(id).id());
  }

  public int clearChecked() {
    int before = items.size();
    items.values().removeIf(item -> !item.isPending());
    return before - items.size();
  }

  private Optional<MarketItem> pendingFor(FoodMetadata food) {
    return items.values().stream()
        .filter(MarketItem::isPending)
        .filter(item -> item.food().key().equals(food.key()))
        .findFirst();
  }

  private MarketItem require(MarketItemId id) {
    MarketItem item = items.get(id);
    if (item == null) {
      throw new IllegalArgumentException("Item is not on the market list");
    }
    return item;
  }

  private MarketItem replace(MarketItem item) {
    items.put(item.id(), item);
    return item;
  }
}
