package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.inventory.ChangeFoodOwnership;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Grant;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Revoke;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.SetVisibility;
import dev.haypacomer.application.inventory.ConsumeFood;
import dev.haypacomer.application.inventory.DiscardFood;
import dev.haypacomer.application.inventory.StockCommand;
import dev.haypacomer.application.inventory.StockFood;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.TrayId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/items")
public class InventoryController {

  private final StockFood stockFood;
  private final ConsumeFood consumeFood;
  private final DiscardFood discardFood;
  private final ChangeFoodOwnership changeOwnership;

  public InventoryController(
      StockFood stockFood,
      ConsumeFood consumeFood,
      DiscardFood discardFood,
      ChangeFoodOwnership changeOwnership) {
    this.stockFood = stockFood;
    this.consumeFood = consumeFood;
    this.discardFood = discardFood;
    this.changeOwnership = changeOwnership;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ItemResponse stock(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody StockRequest request) {
    FoodItem item =
        stockFood.stock(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new StockCommand(
                new FridgeId(request.fridgeId()),
                new TrayId(request.trayId()),
                request.food(),
                Grams.of(request.grams()),
                request.tareGrams() == null ? Grams.ZERO : Grams.of(request.tareGrams()),
                request.expiresOn(),
                request.visibility()));
    return ItemResponse.from(item);
  }

  @PostMapping("/{itemId}/consume")
  RemainingResponse consume(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID itemId,
      @Valid @RequestBody ConsumeRequest request) {
    Grams remaining =
        consumeFood.consume(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new FoodItemId(itemId),
            Grams.of(request.grams()),
            MovementSource.MANUAL);
    return new RemainingResponse(itemId, remaining.value());
  }

  @PostMapping("/{itemId}/discard")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void discard(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId, @PathVariable UUID itemId) {
    discardFood.discard(CurrentUser.of(jwt), new HouseholdId(householdId), new FoodItemId(itemId));
  }

  @PutMapping("/{itemId}/visibility")
  OwnershipResponse visibility(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID itemId,
      @Valid @RequestBody VisibilityRequest request) {
    return OwnershipResponse.from(
        changeOwnership.change(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new FoodItemId(itemId),
            new SetVisibility(request.visibility())));
  }

  @PostMapping("/{itemId}/grants")
  OwnershipResponse grant(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID itemId,
      @Valid @RequestBody GrantRequest request) {
    return OwnershipResponse.from(
        changeOwnership.change(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new FoodItemId(itemId),
            new Grant(new UserId(request.userId()))));
  }

  @DeleteMapping("/{itemId}/grants/{userId}")
  OwnershipResponse revoke(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID itemId,
      @PathVariable UUID userId) {
    return OwnershipResponse.from(
        changeOwnership.change(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new FoodItemId(itemId),
            new Revoke(new UserId(userId))));
  }

  record StockRequest(
      @NotNull UUID fridgeId,
      @NotNull UUID trayId,
      @NotBlank String food,
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal grams,
      @DecimalMin("0") BigDecimal tareGrams,
      LocalDate expiresOn,
      Visibility visibility) {}

  record ConsumeRequest(@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal grams) {}

  record VisibilityRequest(@NotNull Visibility visibility) {}

  record GrantRequest(@NotNull UUID userId) {}

  record ItemResponse(
      UUID id, String name, BigDecimal grams, BigDecimal tareGrams, LocalDate expiresOn) {

    static ItemResponse from(FoodItem item) {
      return new ItemResponse(
          item.id().value(),
          item.name(),
          item.quantity().value(),
          item.tare().value(),
          item.expiresOn().orElse(null));
    }
  }

  record RemainingResponse(UUID id, BigDecimal remainingGrams) {}

  record OwnershipResponse(UUID ownerMemberId, Visibility visibility, int grants) {

    static OwnershipResponse from(Ownership ownership) {
      return new OwnershipResponse(
          ownership.owner().value(), ownership.visibility(), ownership.grantees().size());
    }
  }
}
