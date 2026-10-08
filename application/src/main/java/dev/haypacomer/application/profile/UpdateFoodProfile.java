package dev.haypacomer.application.profile;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import java.util.Objects;
import java.util.Set;

public final class UpdateFoodProfile {

  private final GetHousehold households;
  private final FoodProfileRepository profiles;

  public UpdateFoodProfile(HouseholdRepository households, FoodProfileRepository profiles) {
    this.households = new GetHousehold(households);
    this.profiles = Objects.requireNonNull(profiles, "profiles");
  }

  public FoodProfile update(
      UserId actor,
      HouseholdId householdId,
      Diet diet,
      Set<Allergen> allergies,
      Set<String> avoidedFoods) {
    MemberId member = households.get(actor, householdId).membershipOf(actor).orElseThrow().member();
    FoodProfile profile = new FoodProfile(member, diet, allergies, avoidedFoods);
    profiles.save(profile);
    return profile;
  }
}
