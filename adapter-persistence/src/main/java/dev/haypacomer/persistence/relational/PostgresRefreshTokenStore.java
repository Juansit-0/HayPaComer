package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.RefreshTokenStore;
import dev.haypacomer.domain.identity.RefreshToken;
import dev.haypacomer.domain.identity.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresRefreshTokenStore implements RefreshTokenStore {

  private final JdbcClient jdbc;

  public PostgresRefreshTokenStore(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void save(RefreshToken token) {
    jdbc.sql(
            """
            INSERT INTO refresh_tokens (id, user_id, token_hash, family_id, replaced_by,
                                        expires_at, revoked_at, created_at)
            VALUES (:id, :user, :hash, :family, :replacedBy, :expiresAt, :revokedAt, :issuedAt)
            ON CONFLICT (id) DO UPDATE SET
                revoked_at = EXCLUDED.revoked_at,
                replaced_by = EXCLUDED.replaced_by
            """)
        .param("id", token.id())
        .param("user", token.user().value())
        .param("hash", HexFormat.of().parseHex(token.tokenHash()))
        .param("family", token.family())
        .param("replacedBy", token.replacedBy(), Types.OTHER)
        .param("expiresAt", Timestamps.toDatabase(token.expiresAt()))
        .param(
            "revokedAt",
            token.revokedAt() == null ? null : Timestamps.toDatabase(token.revokedAt()),
            Types.TIMESTAMP_WITH_TIMEZONE)
        .param("issuedAt", Timestamps.toDatabase(token.issuedAt()))
        .update();
  }

  @Override
  public Optional<RefreshToken> findByHash(String tokenHash) {
    return jdbc.sql(
            """
            SELECT id, user_id, token_hash, family_id, created_at, expires_at, revoked_at,
                   replaced_by
            FROM refresh_tokens WHERE token_hash = :hash
            """)
        .param("hash", HexFormat.of().parseHex(tokenHash))
        .query(this::map)
        .optional();
  }

  @Override
  public void revokeFamily(UUID family, Instant at) {
    jdbc.sql(
            """
            UPDATE refresh_tokens SET revoked_at = :at
            WHERE family_id = :family AND revoked_at IS NULL
            """)
        .param("at", Timestamps.toDatabase(at))
        .param("family", family)
        .update();
  }

  private RefreshToken map(ResultSet row, int rowNumber) throws SQLException {
    OffsetDateTime revokedAt = row.getObject("revoked_at", OffsetDateTime.class);
    return new RefreshToken(
        row.getObject("id", UUID.class),
        new UserId(row.getObject("user_id", UUID.class)),
        HexFormat.of().formatHex(row.getBytes("token_hash")),
        row.getObject("family_id", UUID.class),
        Timestamps.read(row, "created_at"),
        Timestamps.read(row, "expires_at"),
        revokedAt == null ? null : revokedAt.toInstant(),
        row.getObject("replaced_by", UUID.class));
  }
}
