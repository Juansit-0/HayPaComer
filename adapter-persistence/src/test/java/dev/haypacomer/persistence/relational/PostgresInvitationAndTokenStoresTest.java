package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Invitation;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.RefreshToken;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserToken;
import dev.haypacomer.domain.identity.UserTokenType;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresInvitationAndTokenStoresTest extends PostgresTestSupport {

  private PostgresInvitationRepository invitations;
  private PostgresUserTokenStore userTokens;
  private User juan;
  private Household household;

  @BeforeEach
  void createData() {
    juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    invitations = new PostgresInvitationRepository(dataSource);
    userTokens = new PostgresUserTokenStore(dataSource);
  }

  private static String hash(int seed) {
    byte[] bytes = new byte[32];
    bytes[0] = (byte) seed;
    return HexFormat.of().formatHex(bytes);
  }

  @Test
  void storesFindsAcceptsAndDeletesInvitations() {
    Invitation ana =
        Invitation.create(
            household.id(),
            new EmailAddress("ana@haypacomer.dev"),
            Role.MEMBER,
            hash(1),
            juan.id(),
            NOW);
    Invitation leo =
        Invitation.create(
            household.id(),
            new EmailAddress("leo@haypacomer.dev"),
            Role.GUEST,
            hash(2),
            juan.id(),
            NOW);
    invitations.save(ana);
    invitations.save(leo);

    assertEquals(ana, invitations.findByHash(hash(1)).orElseThrow());
    assertEquals(2, invitations.findPending(household.id(), NOW).size());

    invitations.save(ana.accept(NOW.plusSeconds(10)));
    invitations.delete(household.id(), leo.id());

    assertTrue(invitations.findPending(household.id(), NOW).isEmpty());
    assertEquals(NOW.plusSeconds(10), invitations.findByHash(hash(1)).orElseThrow().acceptedAt());
    assertEquals(List.of(), invitations.findPending(household.id(), NOW.plus(Duration.ofDays(8))));
  }

  @Test
  void storesSingleUseTokensAndRevokesAllSessions() {
    UserToken token = UserToken.issue(juan.id(), UserTokenType.RESET_PASSWORD, hash(3), NOW);
    userTokens.save(token);

    assertEquals(token, userTokens.findByHash(hash(3)).orElseThrow());
    userTokens.save(token.use(NOW.plusSeconds(5)));
    assertEquals(NOW.plusSeconds(5), userTokens.findByHash(hash(3)).orElseThrow().usedAt());

    PostgresRefreshTokenStore refreshTokens = new PostgresRefreshTokenStore(dataSource);
    refreshTokens.save(
        RefreshToken.issue(juan.id(), hash(4), UUID.randomUUID(), NOW, Duration.ofDays(1)));
    refreshTokens.save(
        RefreshToken.issue(juan.id(), hash(5), UUID.randomUUID(), NOW, Duration.ofDays(1)));
    refreshTokens.revokeAll(juan.id(), NOW);

    assertTrue(refreshTokens.findByHash(hash(4)).orElseThrow().isRevoked());
    assertTrue(refreshTokens.findByHash(hash(5)).orElseThrow().isRevoked());
  }
}
