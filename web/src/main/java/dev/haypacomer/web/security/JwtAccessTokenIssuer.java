package dev.haypacomer.web.security;

import dev.haypacomer.application.auth.AccessToken;
import dev.haypacomer.application.port.AccessTokenIssuer;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

public class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private final JwtEncoder encoder;
  private final JwtProperties properties;

  public JwtAccessTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
    this.encoder = encoder;
    this.properties = properties;
  }

  @Override
  public AccessToken issue(UserId user, Instant now) {
    Instant expiresAt = now.plus(properties.accessTokenTimeToLive());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(user.value().toString())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new AccessToken(value, expiresAt);
  }
}
