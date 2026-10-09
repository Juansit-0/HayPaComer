package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.coldchain.InvestigateColdIncidents;
import dev.haypacomer.domain.coldchain.investigation.ColdEpisode;
import dev.haypacomer.domain.coldchain.investigation.ColdInvestigation;
import dev.haypacomer.domain.coldchain.investigation.FoodAssessment;
import dev.haypacomer.domain.coldchain.investigation.FoodVerdict;
import dev.haypacomer.domain.coldchain.investigation.LikelyCause;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class ColdInvestigationController {

  private final InvestigateColdIncidents investigate;

  public ColdInvestigationController(InvestigateColdIncidents investigate) {
    this.investigate = investigate;
  }

  @GetMapping("/cold-investigation")
  List<InvestigationResponse> investigate(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestParam(required = false) UUID fridgeId,
      @RequestParam(defaultValue = "24") int hours) {
    return investigate
        .investigate(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            Optional.ofNullable(fridgeId).map(FridgeId::new),
            Duration.ofHours(hours))
        .stream()
        .map(InvestigationResponse::from)
        .toList();
  }

  record EpisodeResponse(
      Instant start,
      Instant end,
      BigDecimal peakCelsius,
      long minutesAboveLimit,
      long doorOpenSeconds,
      LikelyCause cause) {

    static EpisodeResponse from(ColdEpisode episode) {
      return new EpisodeResponse(
          episode.start(),
          episode.end(),
          episode.peakCelsius(),
          episode.aboveLimit().toMinutes(),
          episode.doorOpen().toSeconds(),
          episode.cause());
    }
  }

  record FoodResponse(
      UUID itemId, String food, BigDecimal grams, FoodVerdict verdict, String reason) {

    static FoodResponse from(FoodAssessment food) {
      return new FoodResponse(
          food.item().value(), food.food(), food.grams().value(), food.verdict(), food.reason());
    }
  }

  record InvestigationResponse(
      UUID fridgeId,
      Instant from,
      Instant to,
      int readings,
      long minutesAboveLimit,
      List<EpisodeResponse> episodes,
      List<FoodResponse> foods) {

    static InvestigationResponse from(ColdInvestigation investigation) {
      return new InvestigationResponse(
          investigation.fridge().value(),
          investigation.from(),
          investigation.to(),
          investigation.readings(),
          investigation.totalAboveLimit().toMinutes(),
          investigation.episodes().stream().map(EpisodeResponse::from).toList(),
          investigation.foods().stream().map(FoodResponse::from).toList());
    }
  }
}
