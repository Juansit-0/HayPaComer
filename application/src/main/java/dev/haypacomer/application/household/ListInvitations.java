package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InvitationRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Invitation;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class ListInvitations {

  private final MemberHouseholds households;
  private final InvitationRepository invitations;
  private final Clock clock;

  public ListInvitations(
      HouseholdRepository households, InvitationRepository invitations, Clock clock) {
    this.households = new MemberHouseholds(households);
    this.invitations = Objects.requireNonNull(invitations, "invitations");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<Invitation> list(UserId actor, HouseholdId id) {
    households.load(actor, id).requirePermission(actor, Permission.MANAGE_MEMBERS);
    return invitations.findPending(id, clock.instant());
  }
}
