package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.InvitationRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Invitation;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresInvitationRepository implements InvitationRepository {

  private static final String SELECT =
      "SELECT id, household_id, email, role, token_hash, invited_by, expires_at, accepted_at"
          + " FROM household_invitations";

  private final JdbcClient jdbc;

  public PostgresInvitationRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void save(Invitation invitation) {
    jdbc.sql(
            """
            INSERT INTO household_invitations (id, household_id, email, role, token_hash,
                                               invited_by, expires_at, accepted_at)
            VALUES (:id, :household, :email, :role, :hash, :invitedBy, :expiresAt, :acceptedAt)
            ON CONFLICT (id) DO UPDATE SET accepted_at = EXCLUDED.accepted_at
            """)
        .param("id", invitation.id())
        .param("household", invitation.household().value())
        .param("email", invitation.email().value())
        .param("role", invitation.role().name())
        .param("hash", HexFormat.of().parseHex(invitation.tokenHash()))
        .param("invitedBy", invitation.invitedBy().value())
        .param("expiresAt", Timestamps.toDatabase(invitation.expiresAt()))
        .param(
            "acceptedAt",
            invitation.acceptedAt() == null ? null : Timestamps.toDatabase(invitation.acceptedAt()),
            Types.TIMESTAMP_WITH_TIMEZONE)
        .update();
  }

  @Override
  public Optional<Invitation> findByHash(String tokenHash) {
    return jdbc.sql(SELECT + " WHERE token_hash = :hash")
        .param("hash", HexFormat.of().parseHex(tokenHash))
        .query(this::map)
        .optional();
  }

  @Override
  public List<Invitation> findPending(HouseholdId household, Instant now) {
    return jdbc.sql(
            SELECT
                + " WHERE household_id = :household AND accepted_at IS NULL AND expires_at > :now"
                + " ORDER BY expires_at, id")
        .param("household", household.value())
        .param("now", Timestamps.toDatabase(now))
        .query(this::map)
        .list();
  }

  @Override
  public void delete(HouseholdId household, UUID invitationId) {
    jdbc.sql("DELETE FROM household_invitations WHERE id = :id AND household_id = :household")
        .param("id", invitationId)
        .param("household", household.value())
        .update();
  }

  private Invitation map(ResultSet row, int rowNumber) throws SQLException {
    OffsetDateTime acceptedAt = row.getObject("accepted_at", OffsetDateTime.class);
    return new Invitation(
        row.getObject("id", UUID.class),
        new HouseholdId(row.getObject("household_id", UUID.class)),
        new EmailAddress(row.getString("email")),
        Role.valueOf(row.getString("role")),
        HexFormat.of().formatHex(row.getBytes("token_hash")),
        new UserId(row.getObject("invited_by", UUID.class)),
        Timestamps.read(row, "expires_at"),
        acceptedAt == null ? null : acceptedAt.toInstant());
  }
}
