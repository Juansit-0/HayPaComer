package dev.haypacomer.ai.offline;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.StatusExplanation;
import dev.haypacomer.application.ai.Urgency;
import dev.haypacomer.domain.inventory.FoodStatus;
import dev.haypacomer.domain.inventory.StockedFood;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class StatusRules {

  public StatusExplanation explain(StockedFood food, LocalDate today) {
    String name = food.item().name();
    Optional<LocalDate> expiry = food.item().expiresOn();
    if (food.has(FoodStatus.EXPIRED)) {
      return explanation(
          name,
          Urgency.DISCARD,
          name + " expired" + expiry.map(date -> " on " + date).orElse("") + ". Do not eat it.");
    }
    if (food.has(FoodStatus.UNDER_REVIEW)) {
      return explanation(
          name, Urgency.REVIEW, "The fridge got too warm. Check " + name + " before eating it.");
    }
    long days = expiry.map(date -> ChronoUnit.DAYS.between(today, date)).orElse(Long.MAX_VALUE);
    if (food.has(FoodStatus.AT_RISK) && days <= 0) {
      return explanation(name, Urgency.CONSUME_TODAY, "Eat " + name + " today.");
    }
    if (food.has(FoodStatus.AT_RISK)) {
      return explanation(
          name,
          Urgency.CONSUME_SOON,
          name + " expires in " + days + (days == 1 ? " day" : " days") + ". Use it soon.");
    }
    if (food.has(FoodStatus.LEFTOVER)) {
      return explanation(name, Urgency.CONSUME_SOON, "Leftover " + name + ". Plan it for a meal.");
    }
    return explanation(name, Urgency.OK, name + " is fine.");
  }

  private static StatusExplanation explanation(String food, Urgency urgency, String message) {
    return new StatusExplanation(food, urgency, message, AdvisorSource.OFFLINE_RULES);
  }
}
