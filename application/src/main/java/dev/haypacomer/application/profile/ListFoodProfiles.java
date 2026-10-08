package dev.haypacomer.application.profile;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Membership;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.FoodProfile;
import java.util.List;
import java.util.Objects;

public final class ListFoodProfiles {

  private final GetHousehold households;
  private final FoodProfileRepository profiles;

  public ListFoodProfiles(HouseholdRepository households, FoodProfileRepository profiles) {
    this.households = new GetHousehold(households);
    this.profiles = Objects.requireNonNull(profiles, "profiles");
  }

  public List<FoodProfile> list(UserId actor, HouseholdId householdId) {
    return households.get(actor, householdId).memberships().stream()
        .map(Membership::member)
        .map(member -> profiles.find(member).orElseGet(() -> FoodProfile.omnivore(member)))
        .toList();
  }
}
