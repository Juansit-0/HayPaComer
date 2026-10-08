package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.HayPaComerFacade;
import dev.haypacomer.application.audit.ListActivity;
import dev.haypacomer.application.coldchain.ListColdChains;
import dev.haypacomer.application.coldchain.ReviewColdChain;
import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.fridge.ListFridges;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.ChangeFoodOwnership;
import dev.haypacomer.application.inventory.ExecuteInventoryCommand;
import dev.haypacomer.application.inventory.FoodAccessGuard;
import dev.haypacomer.application.inventory.ListSnapshots;
import dev.haypacomer.application.inventory.RestoreSnapshot;
import dev.haypacomer.application.inventory.SearchFoods;
import dev.haypacomer.application.inventory.TakeSnapshot;
import dev.haypacomer.application.inventory.UndoLastChange;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.AuditLog;
import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.application.port.SnapshotStore;
import dev.haypacomer.application.port.UnitOfWork;
import dev.haypacomer.application.quantity.InterpretQuantity;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.sensor.FridgeThresholds;
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
      ColdChainRepository coldChains,
      FreshnessPolicy freshness) {
    return new ViewInventory(households, fridges, ownerships, coldChains, freshness);
  }

  @Bean
  EvaluateRecipe evaluateRecipe(
      HouseholdRepository households, ViewInventory viewInventory, Clock clock) {
    return new EvaluateRecipe(households, viewInventory, clock);
  }

  @Bean
  TrackColdChain trackColdChain(ColdChainRepository chains) {
    return new TrackColdChain(chains, FridgeThresholds.DEFAULT);
  }

  @Bean
  ReviewColdChain reviewColdChain(
      HouseholdRepository households,
      FridgeRepository fridges,
      ColdChainRepository chains,
      Clock clock) {
    return new ReviewColdChain(households, fridges, chains, clock);
  }

  @Bean
  ListColdChains listColdChains(
      HouseholdRepository households, FridgeRepository fridges, ColdChainRepository chains) {
    return new ListColdChains(households, fridges, chains);
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
      SnapshotStore snapshots,
      Clock clock) {
    return new ExecuteInventoryCommand(
        households,
        fridges,
        ownerships,
        movements,
        catalog,
        guard,
        audit,
        unitOfWork,
        snapshots,
        clock);
  }

  @Bean
  UndoLastChange undoLastChange(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      SnapshotStore snapshots,
      AuditLog audit,
      UnitOfWork unitOfWork,
      Clock clock) {
    return new UndoLastChange(
        households, fridges, ownerships, movements, catalog, snapshots, audit, unitOfWork, clock);
  }

  @Bean
  TakeSnapshot takeSnapshot(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      SnapshotStore snapshots,
      AuditLog audit,
      UnitOfWork unitOfWork,
      Clock clock) {
    return new TakeSnapshot(
        households, fridges, ownerships, movements, catalog, snapshots, audit, unitOfWork, clock);
  }

  @Bean
  RestoreSnapshot restoreSnapshot(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      SnapshotStore snapshots,
      AuditLog audit,
      UnitOfWork unitOfWork,
      Clock clock) {
    return new RestoreSnapshot(
        households, fridges, ownerships, movements, catalog, snapshots, audit, unitOfWork, clock);
  }

  @Bean
  ListSnapshots listSnapshots(HouseholdRepository households, SnapshotStore snapshots) {
    return new ListSnapshots(households, snapshots);
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

  @Bean
  InterpretQuantity interpretQuantity(FoodCatalogRepository catalog) {
    return new InterpretQuantity(catalog);
  }
}
