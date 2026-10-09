package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.inventory.EstimateExpiry;
import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExpiryController {

  private final EstimateExpiry estimateExpiry;

  public ExpiryController(EstimateExpiry estimateExpiry) {
    this.estimateExpiry = estimateExpiry;
  }

  @GetMapping("/api/v1/households/{householdId}/expiry-estimate")
  EstimateResponse estimate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestParam String food,
      @RequestParam(defaultValue = "SHELF") ZoneKind zone,
      @RequestParam(defaultValue = "false") boolean opened) {
    ExpiryEstimate estimate =
        estimateExpiry.estimate(
            CurrentUser.of(jwt), new HouseholdId(householdId), food, zone, opened);
    return new EstimateResponse(
        estimate.date(), estimate.source(), estimate.confidence(), estimate.shelfDays());
  }

  record EstimateResponse(
      LocalDate expiresOn, ExpirySource source, double confidence, int shelfDays) {}
}
