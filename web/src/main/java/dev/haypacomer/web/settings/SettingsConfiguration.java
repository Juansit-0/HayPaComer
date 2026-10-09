package dev.haypacomer.web.settings;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.SettingsRepository;
import dev.haypacomer.application.settings.ChangeHouseholdSetting;
import dev.haypacomer.application.settings.ListSettingDefinitions;
import dev.haypacomer.application.settings.StoredPolicies;
import dev.haypacomer.application.settings.ViewHouseholdSettings;
import dev.haypacomer.persistence.relational.PostgresSettingsRepository;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SettingsConfiguration {

  @Bean
  SettingsRepository settingsRepository(
      DataSource dataSource,
      Clock clock,
      @Value("${haypacomer.settings.cache-ttl:PT30S}") Duration timeToLive) {
    return new PostgresSettingsRepository(dataSource, clock, timeToLive);
  }

  @Bean
  PolicySource policySource(SettingsRepository settings) {
    return new StoredPolicies(settings);
  }

  @Bean
  ViewHouseholdSettings viewHouseholdSettings(
      HouseholdRepository households, SettingsRepository settings) {
    return new ViewHouseholdSettings(households, settings);
  }

  @Bean
  ChangeHouseholdSetting changeHouseholdSetting(
      HouseholdRepository households, SettingsRepository settings) {
    return new ChangeHouseholdSetting(households, settings);
  }

  @Bean
  ListSettingDefinitions listSettingDefinitions(SettingsRepository settings) {
    return new ListSettingDefinitions(settings);
  }
}
