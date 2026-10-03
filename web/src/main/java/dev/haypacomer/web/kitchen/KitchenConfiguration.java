package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.HayPaComerFacade;
import dev.haypacomer.application.fridge.ListFridges;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.ChangeFoodOwnership;
import dev.haypacomer.application.inventory.ConsumeFood;
import dev.haypacomer.application.inventory.DiscardFood;
import dev.haypacomer.application.inventory.FoodAccessGuard;
import dev.haypacomer.application.inventory.SearchFoods;
import dev.haypacomer.application.inventory.StockFood;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KitchenConfiguration {

  @Bean
  FreshnessPolicy freshnessPolicy() {
    return FreshnessPolicy.DEFAULT;
  }

  @Bean
  SetUpFridge setUpFridge(HouseholdRepository households, FridgeRepository fridges) {
    return new SetUpFridge(households, fridges);
  }

  @Bean
  ListFridges listFridges(HouseholdRepository households, FridgeRepository fridges) {
    return new ListFridges(households, fridges);
  }

  @Bean
  ViewInventory viewInventory(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      FreshnessPolicy freshness) {
    return new ViewInventory(households, fridges, ownerships, freshness);
  }

  @Bean
  StockFood stockFood(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      Clock clock) {
    return new StockFood(households, fridges, ownerships, movements, catalog, clock);
  }

  @Bean
  ConsumeFood consumeFood(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodAccessGuard guard,
      Clock clock) {
    return new ConsumeFood(households, fridges, ownerships, movements, guard, clock);
  }

  @Bean
  DiscardFood discardFood(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodAccessGuard guard,
      Clock clock) {
    return new DiscardFood(households, fridges, ownerships, movements, guard, clock);
  }

  @Bean
  ChangeFoodOwnership changeFoodOwnership(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements) {
    return new ChangeFoodOwnership(households, fridges, ownerships, movements);
  }

  @Bean
  SearchFoods searchFoods(FoodCatalogRepository catalog) {
    return new SearchFoods(catalog);
  }

  @Bean
  HayPaComerFacade hayPaComerFacade(
      GetHousehold getHousehold,
      SetUpFridge setUpFridge,
      ListFridges listFridges,
      ViewInventory viewInventory,
      Clock clock) {
    return new HayPaComerFacade(getHousehold, setUpFridge, listFridges, viewInventory, clock);
  }
}
