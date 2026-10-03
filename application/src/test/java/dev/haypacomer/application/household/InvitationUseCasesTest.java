package dev.haypacomer.application.household;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.auth.InvalidTokenException;
import dev.haypacomer.application.auth.OpaqueTokens;
import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.application.mail.MailLinks;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Invitation;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.PasswordHash;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvitationUseCasesTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  private final InMemoryHouseholds households = new InMemoryHouseholds();
  private final InMemoryInvitations invitations = new InMemoryInvitations();
  private final List<EmailMessage> outbox = new ArrayList<>();
  private final OpaqueTokens opaqueTokens = new OpaqueTokens();
  private final MailLinks links = new MailLinks("https://haypacomer.dev");
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final Map<UserId, User> users = new HashMap<>();
  private User juan;
  private User ana;
  private Household household;

  private User user(String email) {
    User user = User.register(new EmailAddress(email), new PasswordHash("hash"), "Person", NOW);
    users.put(user.id(), user);
    return user;
  }

  private UserRepository userRepository() {
    return new UserRepository() {
      @Override
      public void save(User user) {
        users.put(user.id(), user);
      }

      @Override
      public Optional<User> findById(UserId id) {
        return Optional.ofNullable(users.get(id));
      }

      @Override
      public Optional<User> findByEmail(EmailAddress email) {
        return users.values().stream().filter(user -> user.email().equals(email)).findFirst();
      }
    };
  }

  private InviteMember invite() {
    return new InviteMember(households, invitations, opaqueTokens, outbox::add, links, clock);
  }

  private AcceptInvitation accept(Clock at) {
    return new AcceptInvitation(invitations, households, userRepository(), opaqueTokens, at);
  }

  private String lastToken() {
    String link = outbox.getLast().link();
    return URLDecoder.decode(link.substring(link.indexOf("#token=") + 7), StandardCharsets.UTF_8);
  }

  @BeforeEach
  void createHousehold() {
    juan = user("juan@haypacomer.dev");
    ana = user("ana@haypacomer.dev");
    household =
        new CreateHousehold(households, clock)
            .create(juan.id(), new CreateHouseholdCommand("Apartment 402", "COP", "UTC"));
  }

  @Test
  void ownerInvitesAndTheInviteeJoinsWithTheRole() {
    Invitation invitation =
        invite().invite(juan.id(), household.id(), "ANA@haypacomer.dev", Role.MEMBER);

    assertEquals(new EmailAddress("ana@haypacomer.dev"), outbox.getLast().to());
    assertTrue(outbox.getLast().link().startsWith("https://haypacomer.dev/join#token="));
    assertEquals(
        List.of(invitation),
        new ListInvitations(households, invitations, clock).list(juan.id(), household.id()));

    Household joined = accept(clock).accept(ana.id(), lastToken());

    assertEquals(Role.MEMBER, joined.membershipOf(ana.id()).orElseThrow().role());
    assertTrue(
        new ListInvitations(households, invitations, clock)
            .list(juan.id(), household.id())
            .isEmpty());
  }

  @Test
  void onlyMembersManagersCanInviteListOrCancel() {
    household.join(ana.id(), Role.MEMBER, NOW);

    assertThrows(
        AccessDeniedException.class,
        () -> invite().invite(ana.id(), household.id(), "x@haypacomer.dev", Role.GUEST));
    assertThrows(
        AccessDeniedException.class,
        () -> new ListInvitations(households, invitations, clock).list(ana.id(), household.id()));
    assertThrows(
        HouseholdNotFoundException.class,
        () ->
            invite()
                .invite(
                    user("z@haypacomer.dev").id(), household.id(), "x@haypacomer.dev", Role.GUEST));
  }

  @Test
  void cancelledInvitationsCannotBeAccepted() {
    Invitation invitation =
        invite().invite(juan.id(), household.id(), "ana@haypacomer.dev", Role.GUEST);

    new CancelInvitation(households, invitations)
        .cancel(juan.id(), household.id(), invitation.id());

    assertThrows(InvalidTokenException.class, () -> accept(clock).accept(ana.id(), lastToken()));
  }

  @Test
  void invitationIsPersonalSingleUseAndExpires() {
    invite().invite(juan.id(), household.id(), "ana@haypacomer.dev", Role.MEMBER);
    String token = lastToken();
    User mallory = user("mallory@haypacomer.dev");

    assertThrows(
        InvitationEmailMismatchException.class, () -> accept(clock).accept(mallory.id(), token));
    assertThrows(
        InvalidTokenException.class,
        () -> accept(Clock.offset(clock, Duration.ofDays(8))).accept(ana.id(), token));
    accept(clock).accept(ana.id(), token);
    assertThrows(InvalidTokenException.class, () -> accept(clock).accept(ana.id(), token));
    assertThrows(InvalidTokenException.class, () -> accept(clock).accept(ana.id(), " "));
  }
}
