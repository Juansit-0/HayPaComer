package dev.haypacomer.application.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalyticsUseCasesTest {

  private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final Map<String, BigDecimal> saved = new HashMap<>();
  private final FoodPriceRepository prices =
      new FoodPriceRepository() {
        @Override
        public Map<String, BigDecimal> pricesFor(HouseholdId household, Currency currency) {
          Map<String, BigDecimal> all = new HashMap<>(Map.of("rice", new BigDecimal("4800")));
          all.putAll(saved);
          return all;
        }

        @Override
        public void save(HouseholdId household, String foodKey, BigDecimal pricePerKg) {
          saved.put(foodKey, pricePerKg);
        }
      };
  private final UserId owner = UserId.newId();
  private final UserId guest = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), BOGOTA, owner, Instant.now());

  {
    home.join(guest, Role.GUEST, Instant.now());
    households.save(home);
    stores.catalog.save(
        new FoodMetadata(
            "Chicken breast",
            FoodCategory.POULTRY,
            Unit.GRAM,
            ConversionFactors.MASS_ONLY,
            true,
            2,
            Set.of()));
  }

  private void moved(MovementType type, long grams, String food, LocalDate day, LocalDate expiry) {
    stores.movements.record(
        new InventoryMovement(
            UUID.randomUUID(),
            home.id(),
            FoodItemId.newId(),
            owner,
            type,
            BigDecimal.valueOf(-grams),
            MovementSource.SCALE,
            day.atTime(23, 30).atZone(BOGOTA).toInstant(),
            food,
            expiry));
  }

  @Test
  void metricsUseTheHouseholdTimezoneCurrencyAndPrices() {
    moved(MovementType.CONSUME, 500, "chicken breast", TODAY, TODAY.plusDays(1));
    moved(MovementType.DISCARD, 1000, "rice", TODAY, null);
    moved(MovementType.CONSUME, 900, "rice", TODAY.minusDays(40), null);
    new SetFoodPrice(households, stores.catalog, prices)
        .set(owner, home.id(), "chicken breast", new BigDecimal("20000.004"));
    ViewHouseholdMetrics view =
        new ViewHouseholdMetrics(households, stores.history, prices, FreshnessPolicy.DEFAULT);

    MetricsReport report = view.view(guest, home.id(), TODAY.minusDays(29), TODAY);

    assertEquals(Currency.getInstance("COP"), report.currency());
    assertEquals(Grams.of(500), report.metrics().total().rescued());
    assertEquals(new BigDecimal("10000.00"), report.metrics().moneySaved());
    assertEquals(new BigDecimal("4800.00"), report.metrics().moneyWasted());
    assertEquals(30, report.metrics().days().size());
    assertEquals(
        new BigDecimal("20000.00"),
        new ListFoodPrices(households, prices).list(guest, home.id()).get("chicken breast"));
  }

  @Test
  void periodsPricesAndPermissionsAreChecked() {
    ViewHouseholdMetrics view =
        new ViewHouseholdMetrics(households, stores.history, prices, FreshnessPolicy.DEFAULT);
    SetFoodPrice set = new SetFoodPrice(households, stores.catalog, prices);

    assertThrows(
        IllegalArgumentException.class,
        () -> view.view(owner, home.id(), TODAY, TODAY.minusDays(1)));
    assertThrows(
        IllegalArgumentException.class,
        () -> view.view(owner, home.id(), TODAY.minusDays(366), TODAY));
    assertThrows(
        HouseholdNotFoundException.class, () -> view.view(UserId.newId(), home.id(), TODAY, TODAY));
    assertThrows(
        AccessDeniedException.class,
        () -> set.set(guest, home.id(), "chicken breast", BigDecimal.TEN));
    assertThrows(
        IllegalArgumentException.class,
        () -> set.set(owner, home.id(), "chicken breast", BigDecimal.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> set.set(owner, home.id(), "chicken breast", new BigDecimal("100000001")));
    assertThrows(
        FoodNotInCatalogException.class, () -> set.set(owner, home.id(), "caviar", BigDecimal.TEN));
  }
}
