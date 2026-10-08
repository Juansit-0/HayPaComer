package dev.haypacomer.web.planning;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.planning.AddPlanDeltaToMarketList;
import dev.haypacomer.application.planning.ChangePlanEntry;
import dev.haypacomer.application.planning.CloneRecipe;
import dev.haypacomer.application.planning.CloneWeeklyPlan;
import dev.haypacomer.application.planning.GenerateWeeklyPlan;
import dev.haypacomer.application.planning.ListRecipeTemplates;
import dev.haypacomer.application.planning.ListRecipes;
import dev.haypacomer.application.planning.SaveRecipe;
import dev.haypacomer.application.planning.ViewCurrentPlan;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.application.port.RecipeTemplateRepository;
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
  ListRecipeTemplates listRecipeTemplates(RecipeTemplateRepository templates) {
    return new ListRecipeTemplates(templates);
  }

  @Bean
  CloneRecipe cloneRecipe(
      HouseholdRepository households,
      RecipeRepository recipes,
      RecipeTemplateRepository templates) {
    return new CloneRecipe(households, recipes, templates);
  }

  @Bean
  CloneWeeklyPlan cloneWeeklyPlan(HouseholdRepository households, WeeklyPlanRepository plans) {
    return new CloneWeeklyPlan(households, plans);
  }

  @Bean
  AddPlanDeltaToMarketList addPlanDeltaToMarketList(
      HouseholdRepository households,
      WeeklyPlanRepository plans,
      ViewInventory inventory,
      MarketListRepository lists,
      Clock clock) {
    return new AddPlanDeltaToMarketList(households, plans, inventory, lists, clock);
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
