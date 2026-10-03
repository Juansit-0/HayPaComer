package dev.haypacomer.domain.member;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.recipe.Recipe;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record DiningGroup(List<FoodProfile> profiles) {

  public DiningGroup {
    profiles = List.copyOf(profiles);
    Set<MemberId> members = new HashSet<>();
    for (FoodProfile profile : profiles) {
      if (!members.add(profile.member())) {
        throw new IllegalArgumentException("Duplicate profile for member " + profile.member());
      }
    }
  }

  public static DiningGroup of(FoodProfile... profiles) {
    return new DiningGroup(List.of(profiles));
  }

  public boolean allows(FoodMetadata food) {
    return profiles.stream().allMatch(profile -> profile.allows(food));
  }

  public List<ProfileConflict> conflictsWith(Recipe recipe) {
    return profiles.stream().flatMap(profile -> profile.conflictsWith(recipe).stream()).toList();
  }

  public boolean canShare(Recipe recipe) {
    return conflictsWith(recipe).isEmpty();
  }
}
