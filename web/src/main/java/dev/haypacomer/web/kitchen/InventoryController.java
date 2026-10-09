package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.inventory.ChangeFoodOwnership;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Grant;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Revoke;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.SetVisibility;
import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.application.inventory.ConsumeFoodCommand;
import dev.haypacomer.application.inventory.DiscardFoodCommand;
import dev.haypacomer.application.inventory.ExecuteInventoryCommand;
import dev.haypacomer.application.inventory.MarkFoodOpened;
import dev.haypacomer.application.inventory.StockFoodCommand;
import dev.haypacomer.domain.expiry.ExpirySource;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/items")
public class InventoryController {

  static final String IDEMPOTENCY_KEY = "Idempotency-Key";

  private final ExecuteInventoryCommand commands;
  private final ChangeFoodOwnership changeOwnership;
  private final MarkFoodOpened markFoodOpened;

  public InventoryController(
      ExecuteInventoryCommand commands,
      ChangeFoodOwnership changeOwnership,
      MarkFoodOpened markFoodOpened) {
    this.commands = commands;
    this.changeOwnership = changeOwnership;
    this.markFoodOpened = markFoodOpened;
  }

  @PostMapping("/{itemId}/open")
  OpenedResponse open(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId, @PathVariable UUID itemId) {
    FoodItem item =
        markFoodOpened.open(
            CurrentUser.of(jwt), new HouseholdId(householdId), new FoodItemId(itemId));
    return new OpenedResponse(
        item.id().value(),
        item.expiresOn().orElse(null),
        item.expirySource().orElse(null),
        item.openedOn().orElse(null));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  OutcomeResponse stock(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestHeader(name = IDEMPOTENCY_KEY, required = false) UUID idempotencyKey,
      @Valid @RequestBody StockRequest request) {
    return OutcomeResponse.from(
        commands.execute(
            CurrentUser.of(jwt),
            new StockFoodCommand(
                commandId(idempotencyKey),
                new HouseholdId(householdId),
                new FridgeId(request.fridgeId()),
                new TrayId(request.trayId()),
                request.food(),
                Grams.of(request.grams()),
                request.tareGrams() == null ? Grams.ZERO : Grams.of(request.tareGrams()),
                request.expiresOn(),
                request.visibility(),
                Boolean.TRUE.equals(request.opened()),
                request.expirySource())));
  }

  @PostMapping("/{itemId}/consume")
  OutcomeResponse consume(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID itemId,
      @RequestHeader(name = IDEMPOTENCY_KEY, required = false) UUID idempotencyKey,
      @Valid @RequestBody ConsumeRequest request) {
    return OutcomeResponse.from(
        commands.execute(
            CurrentUser.of(jwt),
            new ConsumeFoodCommand(
                commandId(idempotencyKey),
                new HouseholdId(householdId),
                new FoodItemId(itemId),
                Grams.of(request.grams()),
                MovementSource.MANUAL)));
  }

  @PostMapping("/{itemId}/discard")
  OutcomeResponse discard(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID itemId,
      @RequestHeader(name = IDEMPOTENCY_KEY, required = false) UUID idempotencyKey) {
    return OutcomeResponse.from(
        commands.execute(
            CurrentUser.of(jwt),
            new DiscardFoodCommand(
                commandId(idempotencyKey), new HouseholdId(householdId), new FoodItemId(itemId))));
  }

  private static UUID commandId(UUID idempotencyKey) {
    return idempotencyKey == null ? UUID.randomUUID() : idempotencyKey;
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
      Visibility visibility,
      Boolean opened,
      ExpirySource expirySource) {}

  record OpenedResponse(
      UUID itemId, LocalDate expiresOn, ExpirySource expirySource, LocalDate openedOn) {}

  record ConsumeRequest(@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal grams) {}

  record VisibilityRequest(@NotNull Visibility visibility) {}

  record GrantRequest(@NotNull UUID userId) {}

  record OutcomeResponse(UUID commandId, UUID itemId, BigDecimal remainingGrams, boolean replayed) {

    static OutcomeResponse from(CommandOutcome outcome) {
      return new OutcomeResponse(
          outcome.commandId(),
          outcome.item().value(),
          outcome.remaining().value(),
          outcome.replayed());
    }
  }

  record OwnershipResponse(UUID ownerMemberId, Visibility visibility, int grants) {

    static OwnershipResponse from(Ownership ownership) {
      return new OwnershipResponse(
          ownership.owner().value(), ownership.visibility(), ownership.grantees().size());
    }
  }
}
