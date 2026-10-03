package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.HayPaComerFacade;
import dev.haypacomer.application.audit.ListActivity;
import dev.haypacomer.application.fridge.ListFridges;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.ChangeFoodOwnership;
import dev.haypacomer.application.inventory.ExecuteInventoryCommand;
import dev.haypacomer.application.inventory.FoodAccessGuard;
import dev.haypacomer.application.inventory.SearchFoods;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.AuditLog;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.application.port.UnitOfWork;
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
  ExecuteInventoryCommand executeInventoryCommand(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      FoodAccessGuard guard,
      AuditLog audit,
      UnitOfWork unitOfWork,
      Clock clock) {
    return new ExecuteInventoryCommand(
        households, fridges, ownerships, movements, catalog, guard, audit, unitOfWork, clock);
  }

  @Bean
  ListActivity listActivity(HouseholdRepository households, AuditLog audit) {
    return new ListActivity(households, audit);
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
