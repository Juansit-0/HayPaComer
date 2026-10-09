package dev.haypacomer.application.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.MarketBudgetRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketBudgetTest {

  private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");
  private static final FoodMetadata MILK =
      new FoodMetadata(
          "Milk", FoodCategory.DAIRY, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 7, Set.of());

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final Map<HouseholdId, BigDecimal> saved = new HashMap<>();
  private final MarketBudgetRepository budgets =
      new MarketBudgetRepository() {
        @Override
        public Optional<BigDecimal> monthly(HouseholdId household) {
          return Optional.ofNullable(saved.get(household));
        }

        @Override
        public void save(HouseholdId household, BigDecimal monthly) {
          saved.put(household, monthly);
        }
      };
  private final UserId owner = UserId.newId();
  private final UserId guest = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), BOGOTA, owner, NOW);
  private final MarketList list = MarketList.empty(home.id());
  private final MarketListRepository lists =
      new MarketListRepository() {
        @Override
        public void save(MarketList updated) {}

        @Override
        public Optional<MarketList> findByHousehold(HouseholdId household) {
          return Optional.of(list).filter(found -> found.household().equals(household));
        }
      };
  private final FoodPriceRepository prices =
      new FoodPriceRepository() {
        @Override
        public Map<String, BigDecimal> pricesFor(HouseholdId household, Currency currency) {
          return Map.of("milk", new BigDecimal("4800"));
        }

        @Override
        public void save(HouseholdId household, String foodKey, BigDecimal pricePerKg) {}
      };
  private final SubstitutionRuleRepository rules =
      new SubstitutionRuleRepository() {
        @Override
        public void save(SubstitutionRule rule) {}

        @Override
        public List<SubstitutionRule> all() {
          return List.of();
        }
      };
  private final ViewMarketBudget view =
      new ViewMarketBudget(households, budgets, lists, prices, rules, Clock.fixed(NOW, BOGOTA));
  private final SetMarketBudget set = new SetMarketBudget(households, budgets);

  {
    home.join(guest, Role.GUEST, NOW);
    households.save(home);
  }

  @Test
  void theMonthStartsInTheHouseholdTimezoneAndCountsWhatWasBought() {
    var bought = list.add(MILK, Grams.of(1000), MarketSource.MANUAL, owner, NOW);
    list.check(bought.id(), Instant.parse("2026-10-01T06:00:00Z"));
    var lastMonth = list.add(MILK, Grams.of(9000), MarketSource.MANUAL, owner, NOW);
    list.check(lastMonth.id(), Instant.parse("2026-10-01T04:59:00Z"));
    list.add(MILK, Grams.of(2000), MarketSource.MANUAL, owner, NOW);

    assertThrows(MarketBudgetNotSetException.class, () -> view.view(guest, home.id()));
    assertEquals(
        new BigDecimal("100000.00"), set.set(owner, home.id(), new BigDecimal("99999.999")));

    BudgetReport report = view.view(guest, home.id());

    assertEquals(Currency.getInstance("COP"), report.currency());
    assertEquals(new BigDecimal("4800.00"), report.plan().spent());
    assertEquals(new BigDecimal("9600.00"), report.plan().plannedCost());
  }

  @Test
  void onlyMembersWhoShopCanSetARealisticBudget() {
    assertThrows(AccessDeniedException.class, () -> set.set(guest, home.id(), BigDecimal.TEN));
    assertThrows(IllegalArgumentException.class, () -> set.set(owner, home.id(), BigDecimal.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> set.set(owner, home.id(), new BigDecimal("10000000001")));
  }
}
