package dev.haypacomer.web.market;

import dev.haypacomer.application.market.AddToMarketList;
import dev.haypacomer.application.market.UpdateMarketList;
import dev.haypacomer.application.market.ViewMarketList;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketListRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketConfiguration {

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
