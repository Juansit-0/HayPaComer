package dev.haypacomer.web.analytics;

import dev.haypacomer.application.analytics.ExportHouseholdReport;
import dev.haypacomer.application.analytics.ListFoodPrices;
import dev.haypacomer.application.analytics.SetFoodPrice;
import dev.haypacomer.application.analytics.ViewHouseholdMetrics;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MovementHistory;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalyticsConfiguration {

  @Bean
  ViewHouseholdMetrics viewHouseholdMetrics(
      HouseholdRepository households,
      MovementHistory history,
      FoodPriceRepository prices,
      FreshnessPolicy freshness) {
    return new ViewHouseholdMetrics(households, history, prices, freshness);
  }

  @Bean
  SetFoodPrice setFoodPrice(
      HouseholdRepository households, FoodCatalogRepository catalog, FoodPriceRepository prices) {
    return new SetFoodPrice(households, catalog, prices);
  }

  @Bean
  ExportHouseholdReport exportHouseholdReport(
      HouseholdRepository households, UserRepository users, ViewHouseholdMetrics metrics) {
    return new ExportHouseholdReport(households, users, metrics);
  }

  @Bean
  ListFoodPrices listFoodPrices(HouseholdRepository households, FoodPriceRepository prices) {
    return new ListFoodPrices(households, prices);
  }
}
