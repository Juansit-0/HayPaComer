package dev.haypacomer.web.household;

import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Membership;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class HouseholdDtos {

  private HouseholdDtos() {}

  record CreateHouseholdRequest(
      @NotBlank @Size(max = 80) String name,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotBlank String timezone) {}

  record UpdateHouseholdRequest(@Size(max = 80) String name, String currency, String timezone) {}

  record ChangeRoleRequest(@NotNull Role role) {}

  record TransferOwnershipRequest(@NotNull UUID userId) {}

  record MemberResponse(UUID userId, UUID memberId, Role role, Instant joinedAt) {

    static MemberResponse from(Membership membership) {
      return new MemberResponse(
          membership.user().value(),
          membership.member().value(),
          membership.role(),
          membership.joinedAt());
    }
  }

  record HouseholdResponse(
      UUID id,
      String name,
      String currency,
      String timezone,
      Role myRole,
      List<MemberResponse> members) {

    static HouseholdResponse from(Household household, UserId viewer) {
      return new HouseholdResponse(
          household.id().value(),
          household.name(),
          household.currency().getCurrencyCode(),
          household.timezone().getId(),
          household.membershipOf(viewer).map(Membership::role).orElseThrow(),
          household.memberships().stream().map(MemberResponse::from).toList());
    }
  }
}
