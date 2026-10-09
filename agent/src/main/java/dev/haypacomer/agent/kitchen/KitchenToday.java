package dev.haypacomer.agent.kitchen;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

public record KitchenToday(HouseholdRepository households, Clock clock) {

  public LocalDate of(HouseholdId household) {
    return LocalDate.ofInstant(
        clock.instant(),
        households.findById(household).map(found -> found.timezone()).orElse(ZoneOffset.UTC));
  }
}
