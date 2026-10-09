package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.HayPaComerFacade;
import dev.haypacomer.application.ai.SuggestDishes;
import dev.haypacomer.application.audit.ListActivity;
import dev.haypacomer.application.coldchain.InvestigateColdIncidents;
import dev.haypacomer.application.coldchain.ListColdChains;
import dev.haypacomer.application.coldchain.ReviewColdChain;
import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.fridge.ListFridges;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.fridge.UseFridgeSession;
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
import dev.haypacomer.application.live.BroadcastLiveUpdate;
import dev.haypacomer.application.market.AddMissingToMarketList;
import dev.haypacomer.application.port.AiRateLimiter;
import dev.haypacomer.application.port.AuditLog;
import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.application.port.CookingSessionRepository;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.FridgeSessionRegistry;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.application.port.SensorHistory;
import dev.haypacomer.application.port.SnapshotStore;
import dev.haypacomer.application.port.StepTimerStore;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.application.port.UnitOfWork;
import dev.haypacomer.application.profile.ListFoodProfiles;
import dev.haypacomer.application.profile.UpdateFoodProfile;
import dev.haypacomer.application.quantity.InterpretQuantity;
import dev.haypacomer.application.scale.ReadWeighingProgress;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.application.session.AdvanceCookingSession;
import dev.haypacomer.application.session.CheckCookingTimers;
import dev.haypacomer.application.session.GuidedCookingMediator;
import dev.haypacomer.application.session.KitchenMediator;
import dev.haypacomer.application.session.ResumeCookingSession;
import dev.haypacomer.application.session.StartCookingSession;
import dev.haypacomer.application.session.ViewCookingSession;
import dev.haypacomer.application.session.ViewStepTimer;
import dev.haypacomer.application.session.WeighStep;
import dev.haypacomer.sensors.cooking.InMemoryStepTimerStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KitchenConfiguration {

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
      PolicySource policies) {
    return new ViewInventory(households, fridges, ownerships, coldChains, policies);
  }

  @Bean
  EvaluateRecipe evaluateRecipe(
      HouseholdRepository households,
      ViewInventory viewInventory,
      FoodProfileRepository profiles,
      SubstitutionRuleRepository rules,
      Clock clock) {
    return new EvaluateRecipe(households, viewInventory, profiles, rules, clock);
  }

  @Bean
  UseFridgeSession useFridgeSession(
      HouseholdRepository households,
      FridgeRepository fridges,
      FridgeSessionRegistry sessions,
      Clock clock) {
    return new UseFridgeSession(households, fridges, sessions, clock);
  }

  @Bean
  SuggestDishes suggestDishes(
      HouseholdRepository households,
      ViewInventory viewInventory,
      FoodProfileRepository profiles,
      SubstitutionRuleRepository rules,
      KitchenAdvisor advisor,
      AiRateLimiter rateLimiter,
      PolicySource policies,
      Clock clock) {
    return new SuggestDishes(
        households, viewInventory, profiles, rules, advisor, rateLimiter, policies, clock);
  }

  @Bean
  AddMissingToMarketList addMissingToMarketList(
      HouseholdRepository households,
      EvaluateRecipe evaluateRecipe,
      MarketListRepository lists,
      Clock clock) {
    return new AddMissingToMarketList(households, evaluateRecipe, lists, clock);
  }

  @Bean
  UpdateFoodProfile updateFoodProfile(
      HouseholdRepository households, FoodProfileRepository profiles) {
    return new UpdateFoodProfile(households, profiles);
  }

  @Bean
  ListFoodProfiles listFoodProfiles(
      HouseholdRepository households, FoodProfileRepository profiles) {
    return new ListFoodProfiles(households, profiles);
  }

  @Bean
  TrackColdChain trackColdChain(ColdChainRepository chains, PolicySource policies) {
    return new TrackColdChain(chains, policies);
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
  InvestigateColdIncidents investigateColdIncidents(
      HouseholdRepository households,
      FridgeRepository fridges,
      SensorHistory history,
      ViewInventory viewInventory,
      PolicySource policies,
      Clock clock) {
    return new InvestigateColdIncidents(
        households, fridges, history, viewInventory, policies, clock);
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
      BroadcastLiveUpdate live,
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
        live,
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

  @Bean
  StepTimerStore stepTimerStore() {
    return new InMemoryStepTimerStore();
  }

  @Bean
  KitchenMediator kitchenMediator(
      ScaleSessionStore scales,
      StepTimerStore timers,
      DeviceRepository devices,
      HardwareFactories hardware) {
    return new GuidedCookingMediator(scales, timers, devices, hardware);
  }

  @Bean
  WeighStep weighStep(
      HouseholdRepository households,
      CookingSessionRepository sessions,
      ReadWeighingProgress scaleProgress,
      KitchenMediator mediator,
      Clock clock) {
    return new WeighStep(households, sessions, scaleProgress, mediator, clock);
  }

  @Bean
  ViewStepTimer viewStepTimer(
      HouseholdRepository households,
      CookingSessionRepository sessions,
      StepTimerStore timers,
      Clock clock) {
    return new ViewStepTimer(households, sessions, timers, clock);
  }

  @Bean
  CheckCookingTimers checkCookingTimers(KitchenMediator mediator, Clock clock) {
    return new CheckCookingTimers(mediator, clock);
  }

  @Bean
  StartCookingSession startCookingSession(
      HouseholdRepository households,
      CookingSessionRepository sessions,
      DeviceRepository devices,
      KitchenMediator mediator,
      Clock clock) {
    return new StartCookingSession(households, sessions, devices, mediator, clock);
  }

  @Bean
  AdvanceCookingSession advanceCookingSession(
      HouseholdRepository households,
      CookingSessionRepository sessions,
      KitchenMediator mediator,
      Clock clock) {
    return new AdvanceCookingSession(households, sessions, mediator, clock);
  }

  @Bean
  ViewCookingSession viewCookingSession(
      HouseholdRepository households, CookingSessionRepository sessions) {
    return new ViewCookingSession(households, sessions);
  }

  @Bean
  ResumeCookingSession resumeCookingSession(
      HouseholdRepository households, CookingSessionRepository sessions) {
    return new ResumeCookingSession(households, sessions);
  }
}
