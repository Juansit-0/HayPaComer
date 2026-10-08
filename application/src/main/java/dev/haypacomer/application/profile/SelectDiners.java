package dev.haypacomer.application.profile;

import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import java.util.List;
import java.util.Set;

public final class SelectDiners {

  private final ListFoodProfiles profiles;

  public SelectDiners(HouseholdRepository households, FoodProfileRepository profiles) {
    this.profiles = new ListFoodProfiles(households, profiles);
  }

  public DiningGroup select(UserId actor, HouseholdId householdId, Set<MemberId> diners) {
    List<FoodProfile> all = profiles.list(actor, householdId);
    if (diners.isEmpty()) {
      return new DiningGroup(all);
    }
    List<FoodProfile> chosen =
        all.stream().filter(profile -> diners.contains(profile.member())).toList();
    if (chosen.size() != diners.size()) {
      throw new IllegalArgumentException("Every diner must be a member of the household");
    }
    return new DiningGroup(chosen);
  }
}
