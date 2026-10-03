package dev.haypacomer.web.kitchen;

import dev.haypacomer.application.HayPaComerFacade;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.kitchen.KitchenDtos.InventoryItemResponse;
import dev.haypacomer.web.kitchen.KitchenDtos.NodeResponse;
import dev.haypacomer.web.kitchen.KitchenDtos.SetUpFridgeRequest;
import dev.haypacomer.web.kitchen.KitchenDtos.SnapshotResponse;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
public class KitchenController {

  private final HayPaComerFacade facade;

  public KitchenController(HayPaComerFacade facade) {
    this.facade = facade;
  }

  @PostMapping("/fridges")
  @ResponseStatus(HttpStatus.CREATED)
  NodeResponse setUpFridge(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody SetUpFridgeRequest request) {
    FridgeLayout layout = request.layout() == null ? FridgeLayout.STANDARD : request.layout();
    return NodeResponse.from(
        facade.setUpFridge(
            CurrentUser.of(jwt), new HouseholdId(householdId), request.name(), layout));
  }

  @GetMapping("/fridges")
  List<NodeResponse> fridges(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return facade.fridges(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(NodeResponse::from)
        .toList();
  }

  @GetMapping("/inventory")
  List<InventoryItemResponse> inventory(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @RequestParam(defaultValue = "false") boolean rescueFirst) {
    HouseholdId household = new HouseholdId(householdId);
    var entries =
        rescueFirst
            ? facade.rescueFirst(CurrentUser.of(jwt), household)
            : facade.inventory(CurrentUser.of(jwt), household);
    return entries.stream().map(InventoryItemResponse::from).toList();
  }

  @GetMapping("/kitchen")
  SnapshotResponse snapshot(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return SnapshotResponse.from(
        facade.snapshot(CurrentUser.of(jwt), new HouseholdId(householdId)));
  }
}
