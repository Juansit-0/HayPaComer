package dev.haypacomer.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.auth.AccessToken;
import dev.haypacomer.domain.identity.PasswordHash;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtAccessTokenIssuerTest {

  private static final String SECRET = "test-secret-with-at-least-32-bytes!!";
  private static final String ISSUER = "https://api.haypacomer.dev";

  private final SecurityConfiguration configuration = new SecurityConfiguration();
  private final JwtProperties properties =
      new JwtProperties(SECRET, ISSUER, Duration.ofMinutes(15));
  private final JwtAccessTokenIssuer issuer =
      new JwtAccessTokenIssuer(configuration.jwtEncoder(properties), properties);
  private final JwtDecoder decoder = configuration.jwtDecoder(properties);

  @Test
  void issuesSignedTokenThatTheDecoderAccepts() {
    UserId user = UserId.newId();
    Instant now = Instant.now();

    AccessToken token = issuer.issue(user, now);
    Jwt jwt = decoder.decode(token.value());

    assertEquals(user.value().toString(), jwt.getSubject());
    assertEquals(ISSUER, jwt.getClaimAsString("iss"));
    assertEquals(token.expiresAt().getEpochSecond(), jwt.getExpiresAt().getEpochSecond());
    assertTrue(jwt.getId() != null && !jwt.getId().isBlank());
  }

  @Test
  void rejectsTokensSignedWithAnotherSecretOrIssuer() {
    JwtProperties otherSecret =
        new JwtProperties("another-secret-with-at-least-32-bytes", ISSUER, Duration.ofMinutes(15));
    JwtProperties otherIssuer =
        new JwtProperties(SECRET, "https://evil.example", Duration.ofMinutes(15));
    String forged =
        new JwtAccessTokenIssuer(configuration.jwtEncoder(otherSecret), otherSecret)
            .issue(UserId.newId(), Instant.now())
            .value();
    String wrongIssuer =
        new JwtAccessTokenIssuer(configuration.jwtEncoder(otherIssuer), otherIssuer)
            .issue(UserId.newId(), Instant.now())
            .value();

    assertThrows(JwtException.class, () -> decoder.decode(forged));
    assertThrows(JwtException.class, () -> decoder.decode(wrongIssuer));
  }

  @Test
  void rejectsExpiredTokens() {
    String expired = issuer.issue(UserId.newId(), Instant.now().minus(Duration.ofHours(1))).value();

    assertThrows(JwtException.class, () -> decoder.decode(expired));
  }

  @Test
  void propertiesFailFastAndHideTheSecret() {
    assertThrows(
        IllegalStateException.class,
        () -> new JwtProperties("short", ISSUER, Duration.ofMinutes(15)));
    assertThrows(
        IllegalStateException.class, () -> new JwtProperties(null, ISSUER, Duration.ofMinutes(15)));
    assertThrows(
        IllegalStateException.class, () -> new JwtProperties(SECRET, " ", Duration.ofMinutes(15)));
    assertThrows(
        IllegalStateException.class, () -> new JwtProperties(SECRET, ISSUER, Duration.ZERO));
    assertFalse(properties.toString().contains(SECRET));
  }

  @Test
  void bcryptHasherMatchesOnlyTheOriginalPassword() {
    BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    PasswordHash hash = hasher.hash("fresh-milk-842");

    assertTrue(hash.value().startsWith("$2a$12$"));
    assertTrue(hasher.matches("fresh-milk-842", hash));
    assertFalse(hasher.matches("fresh-milk-843", hash));
  }
}
