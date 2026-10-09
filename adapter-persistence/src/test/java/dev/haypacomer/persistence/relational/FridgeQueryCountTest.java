package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.CHICKEN;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.EGG;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.quantity.Grams;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FridgeQueryCountTest extends PostgresTestSupport {

  private final AtomicInteger statements = new AtomicInteger();
  private Household household;
  private PostgresFridgeRepository fridges;

  private DataSource counting(DataSource real) {
    return (DataSource)
        Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] {DataSource.class},
            (proxy, method, args) -> {
              Object result = method.invoke(real, args);
              if (result instanceof Connection connection) {
                return Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[] {Connection.class},
                    countStatements(connection));
              }
              return result;
            });
  }

  private InvocationHandler countStatements(Connection connection) {
    return (proxy, method, args) -> {
      if (method.getName().startsWith("prepare") || method.getName().equals("createStatement")) {
        statements.incrementAndGet();
      }
      try {
        return method.invoke(connection, args);
      } catch (java.lang.reflect.InvocationTargetException failure) {
        throw failure.getCause();
      }
    };
  }

  @BeforeEach
  void createHousehold() {
    User juan = user("count@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    PostgresFoodCatalogRepository catalog = new PostgresFoodCatalogRepository(dataSource);
    List.of(MILK, EGG, CHICKEN).forEach(catalog::save);
    fridges = new PostgresFridgeRepository(counting(dataSource));
  }

  private Fridge fridge(String name, int itemsPerTray) {
    Fridge fridge = Fridge.named(name);
    List<FoodMetadata> foods = List.of(MILK, EGG, CHICKEN);
    for (int zoneIndex = 0; zoneIndex < 3; zoneIndex++) {
      Zone zone = Zone.named("Zone " + zoneIndex, ZoneKind.SHELF);
      fridge.add(zone);
      for (int trayIndex = 0; trayIndex < 3; trayIndex++) {
        Tray tray = Tray.named("Tray " + trayIndex, trayIndex);
        zone.add(tray);
        for (int item = 0; item < itemsPerTray; item++) {
          fridge.place(
              new FoodItem(
                  FoodItemId.newId(),
                  foods.get(item % 3),
                  Grams.of(100 + item),
                  Grams.ZERO,
                  item % 2 == 0 ? LocalDate.of(2026, 10, 20) : null),
              tray.id());
        }
      }
    }
    return fridge;
  }

  private int counted(Runnable work) {
    statements.set(0);
    work.run();
    return statements.get();
  }

  @Test
  void loadingAndSavingCostTheSameNumberOfStatementsWhateverTheSize() {
    Fridge small = fridge("Small", 1);
    Fridge big = fridge("Big", 6);

    int saveSmall = counted(() -> fridges.save(household.id(), small));
    int saveBig = counted(() -> fridges.save(household.id(), big));
    int loadOne = counted(() -> fridges.findById(big.id()).orElseThrow());
    int loadAll = counted(() -> fridges.findByHousehold(household.id()));

    assertEquals(saveSmall, saveBig);
    assertTrue(saveBig <= 9, "save used " + saveBig + " statements");
    assertTrue(loadOne <= 3, "load used " + loadOne + " statements");
    assertEquals(loadOne, loadAll);
  }

  @Test
  void theTreeRoundTripsWithOrderEmptyPartsAndRemovals() {
    Fridge big = fridge("Big", 4);
    Zone empty = Zone.named("Empty door", ZoneKind.DOOR);
    big.add(empty);
    fridges.save(household.id(), big);
    Fridge saved = fridges.findById(big.id()).orElseThrow();

    assertEquals(4, saved.children().size());
    assertEquals(36, saved.itemCount());
    assertEquals(big.totalGrams(), saved.totalGrams());
    assertEquals("Zone 0", saved.children().getFirst().name());
    assertTrue(saved.children().getLast().children().isEmpty());

    Tray first = saved.children().getFirst().children().getFirst();
    FoodItem gone = first.children().getFirst();
    saved.take(gone.id());
    fridges.save(household.id(), saved);
    Fridge again = fridges.findById(big.id()).orElseThrow();

    assertTrue(again.findItem(gone.id()).isEmpty());
    assertEquals(35, again.itemCount());
    assertEquals(
        List.of(big.id()),
        fridges.findByHousehold(household.id()).stream().map(Fridge::id).toList());
    assertTrue(fridges.findById(dev.haypacomer.domain.fridge.FridgeId.newId()).isEmpty());
  }

  @Test
  void unknownFoodsAreRejected() {
    Fridge fridge = Fridge.named("Bad");
    Zone zone = Zone.named("Shelf", ZoneKind.SHELF);
    fridge.add(zone);
    Tray tray = Tray.named("Top", 0);
    zone.add(tray);
    fridge.place(
        new FoodItem(
            FoodItemId.newId(),
            new FoodMetadata(
                "Unicorn",
                MILK.category(),
                MILK.defaultUnit(),
                MILK.conversion(),
                true,
                1,
                java.util.Set.of()),
            Grams.of(1),
            Grams.ZERO,
            null),
        tray.id());

    assertThrows(IllegalStateException.class, () -> fridges.save(household.id(), fridge));
  }
}
