package dev.haypacomer.web.market;

import dev.haypacomer.application.market.BudgetReport;
import dev.haypacomer.application.market.SetMarketBudget;
import dev.haypacomer.application.market.ViewMarketBudget;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.market.BudgetLine;
import dev.haypacomer.domain.market.BudgetPlan;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/market-budget")
public class MarketBudgetController {

  private final ViewMarketBudget viewBudget;
  private final SetMarketBudget setBudget;

  public MarketBudgetController(ViewMarketBudget viewBudget, SetMarketBudget setBudget) {
    this.viewBudget = viewBudget;
    this.setBudget = setBudget;
  }

  @GetMapping
  BudgetResponse view(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return BudgetResponse.from(viewBudget.view(CurrentUser.of(jwt), new HouseholdId(householdId)));
  }

  @PutMapping
  BudgetResponse set(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody BudgetRequest request) {
    HouseholdId household = new HouseholdId(householdId);
    setBudget.set(CurrentUser.of(jwt), household, request.monthly());
    return BudgetResponse.from(viewBudget.view(CurrentUser.of(jwt), household));
  }

  record BudgetRequest(@NotNull BigDecimal monthly) {}

  record CheaperResponse(String food, BigDecimal grams, BigDecimal estimatedCost) {}

  record LineResponse(
      UUID itemId,
      String food,
      BigDecimal grams,
      BigDecimal estimatedCost,
      boolean withinBudget,
      CheaperResponse cheaper) {

    static LineResponse from(BudgetLine line) {
      return new LineResponse(
          line.item().id().value(),
          line.item().food().name(),
          line.item().grams().value(),
          line.estimatedCost(),
          line.withinBudget(),
          line.cheaperOption()
              .map(
                  option ->
                      new CheaperResponse(
                          option.substitute().name(),
                          option.grams().value(),
                          option.estimatedCost()))
              .orElse(null));
    }
  }

  record BudgetResponse(
      String currency,
      BigDecimal monthly,
      BigDecimal spent,
      BigDecimal remaining,
      BigDecimal plannedCost,
      List<LineResponse> lines) {

    static BudgetResponse from(BudgetReport report) {
      BudgetPlan plan = report.plan();
      return new BudgetResponse(
          report.currency().getCurrencyCode(),
          plan.monthly(),
          plan.spent(),
          plan.remaining(),
          plan.plannedCost(),
          plan.lines().stream().map(LineResponse::from).toList());
    }
  }
}
