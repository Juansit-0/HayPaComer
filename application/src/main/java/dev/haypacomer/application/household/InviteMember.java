package dev.haypacomer.application.household;

import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.application.mail.MailLinks;
import dev.haypacomer.application.port.EmailSender;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InvitationRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Invitation;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;

public final class InviteMember {

  private final MemberHouseholds households;
  private final InvitationRepository invitations;
  private final OpaqueTokens opaqueTokens;
  private final EmailSender email;
  private final MailLinks links;
  private final Clock clock;

  public InviteMember(
      HouseholdRepository households,
      InvitationRepository invitations,
      OpaqueTokens opaqueTokens,
      EmailSender email,
      MailLinks links,
      Clock clock) {
    this.households = new MemberHouseholds(households);
    this.invitations = Objects.requireNonNull(invitations, "invitations");
    this.opaqueTokens = Objects.requireNonNull(opaqueTokens, "opaqueTokens");
    this.email = Objects.requireNonNull(email, "email");
    this.links = Objects.requireNonNull(links, "links");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Invitation invite(UserId actor, HouseholdId id, String emailAddress, Role role) {
    Household household = households.load(actor, id);
    household.requirePermission(actor, Permission.MANAGE_MEMBERS);
    String raw = opaqueTokens.generate();
    Invitation invitation =
        Invitation.create(
            household.id(),
            new EmailAddress(emailAddress),
            role,
            opaqueTokens.hash(raw),
            actor,
            clock.instant());
    invitations.save(invitation);
    String link = links.to("join", raw);
    email.send(
        new EmailMessage(
            invitation.email(),
            "You are invited to " + household.name() + " on HayPaComer",
            "Join " + household.name() + " within 7 days: " + link,
            link));
    return invitation;
  }
}
