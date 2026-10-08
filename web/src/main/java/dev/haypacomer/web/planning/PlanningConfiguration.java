package dev.haypacomer.web.planning;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.planning.ChangePlanEntry;
import dev.haypacomer.application.planning.GenerateWeeklyPlan;
import dev.haypacomer.application.planning.ListRecipes;
import dev.haypacomer.application.planning.SaveRecipe;
import dev.haypacomer.application.planning.ViewCurrentPlan;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.application.port.WeeklyPlanRepository;
import dev.haypacomer.domain.planning.RescueFirstStrategy;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlanningConfiguration {

  @Bean
  SaveRecipe saveRecipe(HouseholdRepository households, RecipeRepository recipes) {
    return new SaveRecipe(households, recipes);
  }

  @Bean
  ListRecipes listRecipes(HouseholdRepository households, RecipeRepository recipes) {
    return new ListRecipes(households, recipes);
  }

  @Bean
  GenerateWeeklyPlan generateWeeklyPlan(
      HouseholdRepository households,
      ViewInventory inventory,
      FoodProfileRepository profiles,
      RecipeRepository recipes,
      WeeklyPlanRepository plans,
      Clock clock) {
    return new GenerateWeeklyPlan(
        households, inventory, profiles, recipes, plans, new RescueFirstStrategy(), clock);
  }

  @Bean
  ViewCurrentPlan viewCurrentPlan(
      HouseholdRepository households, WeeklyPlanRepository plans, Clock clock) {
    return new ViewCurrentPlan(households, plans, clock);
  }

  @Bean
  ChangePlanEntry changePlanEntry(
      HouseholdRepository households,
      RecipeRepository recipes,
      WeeklyPlanRepository plans,
      EvaluateRecipe evaluate) {
    return new ChangePlanEntry(households, recipes, plans, evaluate);
  }
}
