package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.CHICKEN;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.expiry.ShelfLife;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.FridgeNode;
import dev.haypacomer.domain.fridge.Traversal;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class PostgresFridgeRepositoryTest extends PostgresTestSupport {

  private PostgresFridgeRepository fridges;
  private Household household;
  private Fridge fridge;
  private Tray top;
  private Tray rack;
  private FoodItem milk;
  private FoodItem chicken;

  @BeforeEach
  void createData() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    household =
        Household.create(
            "Apartment 402",
            Currency.getInstance("COP"),
            ZoneId.of("America/Bogota"),
            juan.id(),
            NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    PostgresFoodCatalogRepository catalog = new PostgresFoodCatalogRepository(dataSource);
    catalog.save(MILK);
    catalog.save(CHICKEN);
    fridges = new PostgresFridgeRepository(dataSource);

    fridge = Fridge.named("Kitchen fridge");
    Zone shelves = Zone.named("Shelves", ZoneKind.SHELF);
    Zone door = Zone.named("Door", ZoneKind.DOOR);
    top = Tray.named("Top", 0);
    rack = Tray.named("Rack", 0);
    shelves.add(top);
    shelves.add(Tray.named("Bottom", 1));
    door.add(rack);
    fridge.add(shelves);
    fridge.add(door);
    milk = FoodItem.weighed(MILK, Grams.of(892), Grams.of(50), LocalDate.of(2026, 10, 9));
    chicken = new FoodItem(FoodItemId.newId(), CHICKEN, Grams.of(80), Grams.ZERO, null);
    fridge.place(milk, rack.id());
    fridge.place(chicken, top.id());
  }

  @Test
  void roundTripsTheWholeTree() {
    fridges.save(household.id(), fridge);

    Fridge loaded = fridges.findById(fridge.id()).orElseThrow();

    assertEquals(names(fridge), names(loaded));
    assertEquals(fridge.totalGrams(), loaded.totalGrams());
    FoodItem loadedMilk = loaded.findItem(milk.id()).orElseThrow();
    assertEquals(Grams.of(842), loadedMilk.quantity());
    assertEquals(Grams.of(50), loadedMilk.tare());
    assertEquals(LocalDate.of(2026, 10, 9), loadedMilk.expiresOn().orElseThrow());
    assertEquals(MILK, loadedMilk.food());
    assertTrue(loaded.findItem(chicken.id()).orElseThrow().expiresOn().isEmpty());
  }

  @Test
  void keepsWhereTheDateCameFromAndWhenFoodWasOpened() {
    milk.open(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 7), ExpirySource.ESTIMATED);
    fridges.save(household.id(), fridge);

    FoodItem loaded = fridges.findById(fridge.id()).orElseThrow().findItem(milk.id()).orElseThrow();
    FoodItem undated =
        fridges.findById(fridge.id()).orElseThrow().findItem(chicken.id()).orElseThrow();

    assertEquals(ExpirySource.ESTIMATED, loaded.expirySource().orElseThrow());
    assertEquals(LocalDate.of(2026, 10, 3), loaded.openedOn().orElseThrow());
    assertEquals(LocalDate.of(2026, 10, 7), loaded.expiresOn().orElseThrow());
    assertTrue(undated.expirySource().isEmpty());
    assertTrue(undated.openedOn().isEmpty());

    PostgresShelfLifeCatalog shelfLives = new PostgresShelfLifeCatalog(dataSource);
    assertEquals(ShelfLife.of(MILK), shelfLives.shelfLife(MILK));
    JdbcClient.create(dataSource)
        .sql(
            "UPDATE food_catalog SET fridge_days = 7, door_days = 5, freezer_days = 90,"
                + " opened_days = 4 WHERE name_key = 'milk'")
        .update();
    assertEquals(new ShelfLife(7, 5, 90, 4), shelfLives.shelfLife(MILK));
  }

  @Test
  void persistsConsumptionMovesAndRemovals() {
    fridges.save(household.id(), fridge);

    milk.consume(Grams.of(192));
    fridge.move(chicken.id(), rack.id());
    fridges.save(household.id(), fridge);
    fridge.take(milk.id());
    fridges.save(household.id(), fridge);

    Fridge loaded = fridges.findById(fridge.id()).orElseThrow();
    assertTrue(loaded.findItem(milk.id()).isEmpty());
    assertEquals(rack.id(), loaded.locate(chicken.id()).orElseThrow().id());
    assertEquals(1, loaded.itemCount());
  }

  @Test
  void findsFridgesOfAHousehold() {
    fridges.save(household.id(), fridge);
    fridges.save(household.id(), Fridge.named("Garage fridge"));

    assertEquals(
        List.of("Garage fridge", "Kitchen fridge"),
        fridges.findByHousehold(household.id()).stream().map(Fridge::name).toList());
    assertTrue(fridges.findById(FridgeId.newId()).isEmpty());
  }

  @Test
  void rejectsFoodMissingFromCatalog() {
    FoodMetadata tuna =
        new FoodMetadata(
            "Tuna", FoodCategory.FISH, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 3, Set.of());
    fridge.place(new FoodItem(FoodItemId.newId(), tuna, Grams.of(130), Grams.ZERO, null), top.id());

    assertThrows(IllegalStateException.class, () -> fridges.save(household.id(), fridge));
    assertTrue(fridges.findById(fridge.id()).isEmpty());
  }

  private static List<String> names(Fridge fridge) {
    return fridge.nodes(Traversal.DEPTH_FIRST).map(FridgeNode::name).toList();
  }
}
