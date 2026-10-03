package dev.haypacomer.web.market;

import dev.haypacomer.application.market.AddToMarketList;
import dev.haypacomer.application.market.UpdateMarketList;
import dev.haypacomer.application.market.UpdateMarketList.Change;
import dev.haypacomer.application.market.UpdateMarketList.Check;
import dev.haypacomer.application.market.UpdateMarketList.ClearChecked;
import dev.haypacomer.application.market.UpdateMarketList.Remove;
import dev.haypacomer.application.market.UpdateMarketList.SetGrams;
import dev.haypacomer.application.market.UpdateMarketList.Uncheck;
import dev.haypacomer.application.market.ViewMarketList;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.market.MarketItem;
import dev.haypacomer.domain.market.MarketItemId;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/market-list")
public class MarketController {

  private final ViewMarketList viewMarketList;
  private final AddToMarketList addToMarketList;
  private final UpdateMarketList updateMarketList;

  public MarketController(
      ViewMarketList viewMarketList,
      AddToMarketList addToMarketList,
      UpdateMarketList updateMarketList) {
    this.viewMarketList = viewMarketList;
    this.addToMarketList = addToMarketList;
    this.updateMarketList = updateMarketList;
  }

  @GetMapping
  MarketListResponse view(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return MarketListResponse.from(
        viewMarketList.view(CurrentUser.of(jwt), new HouseholdId(householdId)));
  }

  @PostMapping("/items")
  @ResponseStatus(HttpStatus.CREATED)
  ItemResponse add(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody AddRequest request) {
    return ItemResponse.from(
        addToMarketList.add(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            request.food(),
            Grams.of(request.grams()),
            MarketSource.MANUAL));
  }

  @PatchMapping("/items/{itemId}")
  MarketListResponse changeGrams(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID itemId,
      @Valid @RequestBody GramsRequest request) {
    return apply(
        jwt, householdId, new SetGrams(new MarketItemId(itemId), Grams.of(request.grams())));
  }

  @PostMapping("/items/{itemId}/check")
  MarketListResponse check(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId, @PathVariable UUID itemId) {
    return apply(jwt, householdId, new Check(new MarketItemId(itemId)));
  }

  @DeleteMapping("/items/{itemId}/check")
  MarketListResponse uncheck(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId, @PathVariable UUID itemId) {
    return apply(jwt, householdId, new Uncheck(new MarketItemId(itemId)));
  }

  @DeleteMapping("/items/{itemId}")
  MarketListResponse remove(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId, @PathVariable UUID itemId) {
    return apply(jwt, householdId, new Remove(new MarketItemId(itemId)));
  }

  @DeleteMapping("/checked")
  MarketListResponse clearChecked(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return apply(jwt, householdId, new ClearChecked());
  }

  private MarketListResponse apply(Jwt jwt, UUID householdId, Change change) {
    return MarketListResponse.from(
        updateMarketList.update(CurrentUser.of(jwt), new HouseholdId(householdId), change));
  }

  record AddRequest(
      @NotBlank String food,
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal grams) {}

  record GramsRequest(@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal grams) {}

  record ItemResponse(
      UUID id,
      String food,
      FoodCategory category,
      BigDecimal grams,
      MarketSource source,
      UUID addedBy,
      Instant addedAt,
      Instant checkedAt) {

    static ItemResponse from(MarketItem item) {
      return new ItemResponse(
          item.id().value(),
          item.food().name(),
          item.food().category(),
          item.grams().value(),
          item.source(),
          item.addedBy().value(),
          item.addedAt(),
          item.checkedAt());
    }
  }

  record CategoryGroup(FoodCategory category, List<ItemResponse> items) {}

  record MarketListResponse(List<CategoryGroup> pending, List<ItemResponse> checked) {

    static MarketListResponse from(MarketList list) {
      return new MarketListResponse(
          list.pendingByCategory().entrySet().stream()
              .map(
                  entry ->
                      new CategoryGroup(
                          entry.getKey(),
                          entry.getValue().stream().map(ItemResponse::from).toList()))
              .toList(),
          list.items().stream().filter(item -> !item.isPending()).map(ItemResponse::from).toList());
    }
  }
}
