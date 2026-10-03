package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.coldchain.ListColdChains;
import dev.haypacomer.application.coldchain.ReviewColdChain;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.coldchain.ColdChainPhase;
import dev.haypacomer.domain.coldchain.ColdIncident;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class ColdChainController {

  private final ListColdChains listColdChains;
  private final ReviewColdChain reviewColdChain;

  public ColdChainController(ListColdChains listColdChains, ReviewColdChain reviewColdChain) {
    this.listColdChains = listColdChains;
    this.reviewColdChain = reviewColdChain;
  }

  @GetMapping("/cold-chain")
  List<ColdChainResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return listColdChains.list(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(ColdChainResponse::from)
        .toList();
  }

  @PostMapping("/fridges/{fridgeId}/cold-chain/review")
  IncidentResponse review(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID fridgeId) {
    return IncidentResponse.from(
        reviewColdChain.review(
            CurrentUser.of(jwt), new HouseholdId(householdId), new FridgeId(fridgeId)));
  }

  record ColdChainResponse(
      UUID fridgeId,
      ColdChainPhase phase,
      Instant since,
      BigDecimal peakCelsius,
      boolean recovered,
      BigDecimal lastCelsius,
      Instant lastReadingAt) {

    static ColdChainResponse from(ColdChain chain) {
      return new ColdChainResponse(
          chain.fridge().value(),
          chain.phase(),
          chain.state().since().orElse(null),
          chain.state().peak().orElse(null),
          chain.state().recovered(),
          chain.lastCelsius().orElse(null),
          chain.lastReadingAt().orElse(null));
    }
  }

  record IncidentResponse(Instant startedAt, Instant reviewedAt, BigDecimal peakCelsius) {

    static IncidentResponse from(ColdIncident incident) {
      return new IncidentResponse(
          incident.startedAt(), incident.reviewedAt(), incident.peakCelsius());
    }
  }
}
