package dev.haypacomer.domain.fridge;

import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class FridgeSession {

  public static final Duration IDLE_TIMEOUT = Duration.ofMinutes(3);

  private final FridgeId fridge;
  private UserId user;
  private Instant lastActivity;

  public FridgeSession(FridgeId fridge) {
    this.fridge = Objects.requireNonNull(fridge, "fridge");
  }

  public FridgeId fridge() {
    return fridge;
  }

  public synchronized Optional<UserId> activeUser(Instant now) {
    if (user == null || !now.isBefore(lastActivity.plus(IDLE_TIMEOUT))) {
      return Optional.empty();
    }
    return Optional.of(user);
  }

  public synchronized Optional<Instant> expiresAt(Instant now) {
    return activeUser(now).map(active -> lastActivity.plus(IDLE_TIMEOUT));
  }

  public synchronized void claim(UserId claimant, Instant now) {
    Objects.requireNonNull(claimant, "claimant");
    Objects.requireNonNull(now, "now");
    Optional<UserId> active = activeUser(now);
    if (active.isPresent() && !active.get().equals(claimant)) {
      throw new FridgeBusyException();
    }
    user = claimant;
    lastActivity = now;
  }

  public synchronized void release(UserId leaving, Instant now) {
    if (activeUser(now).filter(leaving::equals).isPresent()) {
      user = null;
      lastActivity = null;
    }
  }
}
