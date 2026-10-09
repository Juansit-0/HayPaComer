package dev.haypacomer.web.market;

import dev.haypacomer.application.market.AddToMarketList;
import dev.haypacomer.application.market.SetMarketBudget;
import dev.haypacomer.application.market.UpdateMarketList;
import dev.haypacomer.application.market.ViewMarketBudget;
import dev.haypacomer.application.market.ViewMarketList;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketBudgetRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketConfiguration {

  @Bean
  SetMarketBudget setMarketBudget(HouseholdRepository households, MarketBudgetRepository budgets) {
    return new SetMarketBudget(households, budgets);
  }

  @Bean
  ViewMarketBudget viewMarketBudget(
      HouseholdRepository households,
      MarketBudgetRepository budgets,
      MarketListRepository lists,
      FoodPriceRepository prices,
      SubstitutionRuleRepository rules,
      Clock clock) {
    return new ViewMarketBudget(households, budgets, lists, prices, rules, clock);
  }

  @Bean
  ViewMarketList viewMarketList(HouseholdRepository households, MarketListRepository lists) {
    return new ViewMarketList(households, lists);
  }

  @Bean
  AddToMarketList addToMarketList(
      HouseholdRepository households,
      MarketListRepository lists,
      FoodCatalogRepository catalog,
      Clock clock) {
    return new AddToMarketList(households, lists, catalog, clock);
  }

  @Bean
  UpdateMarketList updateMarketList(
      HouseholdRepository households, MarketListRepository lists, Clock clock) {
    return new UpdateMarketList(households, lists, clock);
  }
}
