package dev.haypacomer.web.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("haypacomer.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTimeToLive) {

  private static final int MIN_SECRET_BYTES = 32;

  public JwtProperties {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET must be set with at least " + MIN_SECRET_BYTES + " bytes");
    }
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalStateException("JWT issuer must be set");
    }
    if (accessTokenTimeToLive == null
        || accessTokenTimeToLive.isNegative()
        || accessTokenTimeToLive.isZero()) {
      throw new IllegalStateException("Access token lifetime must be positive");
    }
  }

  public SecretKey signingKey() {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Override
  public String toString() {
    return "JwtProperties[issuer="
        + issuer
        + ", accessTokenTimeToLive="
        + accessTokenTimeToLive
        + "]";
  }
}
