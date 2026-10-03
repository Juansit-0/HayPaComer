package dev.haypacomer.application.household;

import dev.haypacomer.application.auth.InvalidTokenException;
import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.auth.UserNotFoundException;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InvitationRepository;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Invitation;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class AcceptInvitation {

  private final InvitationRepository invitations;
  private final HouseholdRepository households;
  private final UserRepository users;
  private final OpaqueTokens opaqueTokens;
  private final Clock clock;

  public AcceptInvitation(
      InvitationRepository invitations,
      HouseholdRepository households,
      UserRepository users,
      OpaqueTokens opaqueTokens,
      Clock clock) {
    this.invitations = Objects.requireNonNull(invitations, "invitations");
    this.households = Objects.requireNonNull(households, "households");
    this.users = Objects.requireNonNull(users, "users");
    this.opaqueTokens = Objects.requireNonNull(opaqueTokens, "opaqueTokens");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Household accept(UserId actor, String rawToken) {
    Instant now = clock.instant();
    if (rawToken == null || rawToken.isBlank()) {
      throw new InvalidTokenException();
    }
    Invitation invitation =
        invitations
            .findByHash(opaqueTokens.hash(rawToken))
            .filter(found -> found.isPending(now))
            .orElseThrow(InvalidTokenException::new);
    User user = users.findById(actor).orElseThrow(UserNotFoundException::new);
    if (!user.email().equals(invitation.email())) {
      throw new InvitationEmailMismatchException();
    }
    Household household =
        households.findById(invitation.household()).orElseThrow(InvalidTokenException::new);
    household.join(actor, invitation.role(), now);
    households.save(household);
    invitations.save(invitation.accept(now));
    return household;
  }
}
