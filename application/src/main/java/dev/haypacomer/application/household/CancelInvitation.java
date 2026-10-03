package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InvitationRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;
import java.util.UUID;

public final class CancelInvitation {

  private final MemberHouseholds households;
  private final InvitationRepository invitations;

  public CancelInvitation(HouseholdRepository households, InvitationRepository invitations) {
    this.households = new MemberHouseholds(households);
    this.invitations = Objects.requireNonNull(invitations, "invitations");
  }

  public void cancel(UserId actor, HouseholdId id, UUID invitationId) {
    households.load(actor, id).requirePermission(actor, Permission.MANAGE_MEMBERS);
    invitations.delete(id, invitationId);
  }
}
