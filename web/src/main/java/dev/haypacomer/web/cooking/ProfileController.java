package dev.haypacomer.web.cooking;

import dev.haypacomer.application.profile.ListFoodProfiles;
import dev.haypacomer.application.profile.UpdateFoodProfile;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class ProfileController {

  private final UpdateFoodProfile updateFoodProfile;
  private final ListFoodProfiles listFoodProfiles;

  public ProfileController(UpdateFoodProfile updateFoodProfile, ListFoodProfiles listFoodProfiles) {
    this.updateFoodProfile = updateFoodProfile;
    this.listFoodProfiles = listFoodProfiles;
  }

  @GetMapping("/profiles")
  List<ProfileResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return listFoodProfiles.list(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(ProfileResponse::from)
        .toList();
  }

  @PutMapping("/profile")
  ProfileResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody ProfileRequest request) {
    return ProfileResponse.from(
        updateFoodProfile.update(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            request.diet(),
            request.allergies() == null ? Set.of() : request.allergies(),
            request.avoidedFoods() == null ? Set.of() : request.avoidedFoods()));
  }

  record ProfileRequest(
      @NotNull Diet diet, Set<Allergen> allergies, @Size(max = 50) Set<String> avoidedFoods) {}

  record ProfileResponse(
      UUID memberId, Diet diet, Set<Allergen> allergies, Set<String> avoidedFoods) {

    static ProfileResponse from(FoodProfile profile) {
      return new ProfileResponse(
          profile.member().value(), profile.diet(), profile.allergies(), profile.avoidedFoods());
    }
  }
}
