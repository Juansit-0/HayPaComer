package dev.haypacomer.web.analytics;

import dev.haypacomer.application.analytics.ListFoodPrices;
import dev.haypacomer.application.analytics.MetricsReport;
import dev.haypacomer.application.analytics.SetFoodPrice;
import dev.haypacomer.application.analytics.ViewHouseholdMetrics;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.domain.analytics.DayTally;
import dev.haypacomer.domain.analytics.FoodTally;
import dev.haypacomer.domain.analytics.HouseholdMetrics;
import dev.haypacomer.domain.analytics.MemberTally;
import dev.haypacomer.domain.analytics.Tally;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class AnalyticsController {

  static final int DEFAULT_DAYS = 30;

  private final ViewHouseholdMetrics viewMetrics;
  private final SetFoodPrice setFoodPrice;
  private final ListFoodPrices listFoodPrices;
  private final GetHousehold getHousehold;
  private final Clock clock;

  public AnalyticsController(
      ViewHouseholdMetrics viewMetrics,
      SetFoodPrice setFoodPrice,
      ListFoodPrices listFoodPrices,
      GetHousehold getHousehold,
      Clock clock) {
    this.viewMetrics = viewMetrics;
    this.setFoodPrice = setFoodPrice;
    this.listFoodPrices = listFoodPrices;
    this.getHousehold = getHousehold;
    this.clock = clock;
  }

  @GetMapping("/analytics")
  MetricsResponse metrics(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    UserId actor = CurrentUser.of(jwt);
    HouseholdId household = new HouseholdId(householdId);
    Household found = getHousehold.get(actor, household);
    LocalDate end = to != null ? to : LocalDate.ofInstant(clock.instant(), found.timezone());
    LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
    return MetricsResponse.from(viewMetrics.view(actor, household, start, end));
  }

  @GetMapping("/prices")
  Map<String, BigDecimal> prices(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return listFoodPrices.list(CurrentUser.of(jwt), new HouseholdId(householdId));
  }

  @PutMapping("/prices")
  PriceResponse setPrice(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody PriceRequest request) {
    FoodMetadata food =
        setFoodPrice.set(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            request.food(),
            request.pricePerKg());
    return new PriceResponse(food.name(), food.key(), request.pricePerKg());
  }

  record PriceRequest(@NotBlank String food, @NotNull BigDecimal pricePerKg) {}

  record PriceResponse(String food, String foodKey, BigDecimal pricePerKg) {}

  record TallyResponse(
      BigDecimal consumedGrams,
      BigDecimal rescuedGrams,
      BigDecimal discardedGrams,
      double wasteRate) {

    static TallyResponse from(Tally tally) {
      return new TallyResponse(
          tally.consumed().value(),
          tally.rescued().value(),
          tally.discarded().value(),
          Math.round(tally.wasteRate() * 1000) / 1000.0);
    }
  }

  record FoodResponse(String foodKey, TallyResponse tally) {

    static FoodResponse from(FoodTally food) {
      return new FoodResponse(food.foodKey(), TallyResponse.from(food.tally()));
    }
  }

  record MemberResponse(UUID userId, TallyResponse tally) {

    static MemberResponse from(MemberTally member) {
      return new MemberResponse(member.user().value(), TallyResponse.from(member.tally()));
    }
  }

  record DayResponse(LocalDate day, TallyResponse tally) {

    static DayResponse from(DayTally day) {
      return new DayResponse(day.day(), TallyResponse.from(day.tally()));
    }
  }

  record MetricsResponse(
      LocalDate from,
      LocalDate to,
      String currency,
      TallyResponse total,
      BigDecimal moneySaved,
      BigDecimal moneyWasted,
      List<FoodResponse> foods,
      List<MemberResponse> members,
      List<DayResponse> days,
      Set<String> unpriced) {

    static MetricsResponse from(MetricsReport report) {
      HouseholdMetrics metrics = report.metrics();
      return new MetricsResponse(
          metrics.from(),
          metrics.to(),
          report.currency().getCurrencyCode(),
          TallyResponse.from(metrics.total()),
          metrics.moneySaved(),
          metrics.moneyWasted(),
          metrics.foods().stream().map(FoodResponse::from).toList(),
          metrics.members().stream().map(MemberResponse::from).toList(),
          metrics.days().stream().map(DayResponse::from).toList(),
          metrics.unpriced());
    }
  }
}
