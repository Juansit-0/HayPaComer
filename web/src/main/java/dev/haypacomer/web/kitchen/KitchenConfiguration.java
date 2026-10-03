package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.HayPaComerFacade;
import dev.haypacomer.application.fridge.ListFridges;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
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
      HouseholdRepository households, FridgeRepository fridges, FreshnessPolicy freshness) {
    return new ViewInventory(households, fridges, freshness);
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
