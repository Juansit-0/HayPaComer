package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.fridge.FridgeSessionAction;
import dev.haypacomer.application.fridge.FridgeSessionStatus;
import dev.haypacomer.application.fridge.UseFridgeSession;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.web.security.CurrentUser;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/fridges/{fridgeId}/session")
public class FridgeSessionController {

  private final UseFridgeSession useFridgeSession;

  public FridgeSessionController(UseFridgeSession useFridgeSession) {
    this.useFridgeSession = useFridgeSession;
  }

  @GetMapping
  SessionResponse view(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID fridgeId) {
    return act(jwt, householdId, fridgeId, FridgeSessionAction.VIEW);
  }

  @PostMapping
  SessionResponse claim(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID fridgeId) {
    return act(jwt, householdId, fridgeId, FridgeSessionAction.CLAIM);
  }

  @DeleteMapping
  SessionResponse release(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID fridgeId) {
    return act(jwt, householdId, fridgeId, FridgeSessionAction.RELEASE);
  }

  private SessionResponse act(
      Jwt jwt, UUID householdId, UUID fridgeId, FridgeSessionAction action) {
    return SessionResponse.from(
        useFridgeSession.apply(
            CurrentUser.of(jwt), new HouseholdId(householdId), new FridgeId(fridgeId), action));
  }

  record SessionResponse(UUID fridgeId, UUID userId, Instant expiresAt) {

    static SessionResponse from(FridgeSessionStatus status) {
      return new SessionResponse(
          status.fridge().value(),
          status.activeUser().map(UserId::value).orElse(null),
          status.expiresAt());
    }
  }
}
