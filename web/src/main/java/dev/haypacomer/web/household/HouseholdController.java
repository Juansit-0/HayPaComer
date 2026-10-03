package dev.haypacomer.web.household;

import dev.haypacomer.application.household.ChangeMemberRole;
import dev.haypacomer.application.household.CreateHousehold;
import dev.haypacomer.application.household.CreateHouseholdCommand;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.household.ListHouseholds;
import dev.haypacomer.application.household.RemoveMember;
import dev.haypacomer.application.household.TransferOwnership;
import dev.haypacomer.application.household.UpdateHousehold;
import dev.haypacomer.application.household.UpdateHouseholdCommand;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.web.household.HouseholdDtos.ChangeRoleRequest;
import dev.haypacomer.web.household.HouseholdDtos.CreateHouseholdRequest;
import dev.haypacomer.web.household.HouseholdDtos.HouseholdResponse;
import dev.haypacomer.web.household.HouseholdDtos.MemberResponse;
import dev.haypacomer.web.household.HouseholdDtos.TransferOwnershipRequest;
import dev.haypacomer.web.household.HouseholdDtos.UpdateHouseholdRequest;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households")
public class HouseholdController {

  private final CreateHousehold createHousehold;
  private final ListHouseholds listHouseholds;
  private final GetHousehold getHousehold;
  private final UpdateHousehold updateHousehold;
  private final ChangeMemberRole changeMemberRole;
  private final RemoveMember removeMember;
  private final TransferOwnership transferOwnership;

  public HouseholdController(
      CreateHousehold createHousehold,
      ListHouseholds listHouseholds,
      GetHousehold getHousehold,
      UpdateHousehold updateHousehold,
      ChangeMemberRole changeMemberRole,
      RemoveMember removeMember,
      TransferOwnership transferOwnership) {
    this.createHousehold = createHousehold;
    this.listHouseholds = listHouseholds;
    this.getHousehold = getHousehold;
    this.updateHousehold = updateHousehold;
    this.changeMemberRole = changeMemberRole;
    this.removeMember = removeMember;
    this.transferOwnership = transferOwnership;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  HouseholdResponse create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateHouseholdRequest request) {
    UserId actor = CurrentUser.of(jwt);
    return HouseholdResponse.from(
        createHousehold.create(
            actor,
            new CreateHouseholdCommand(request.name(), request.currency(), request.timezone())),
        actor);
  }

  @GetMapping
  List<HouseholdResponse> list(@AuthenticationPrincipal Jwt jwt) {
    UserId actor = CurrentUser.of(jwt);
    return listHouseholds.list(actor).stream()
        .map(household -> HouseholdResponse.from(household, actor))
        .toList();
  }

  @GetMapping("/{householdId}")
  HouseholdResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    UserId actor = CurrentUser.of(jwt);
    return HouseholdResponse.from(getHousehold.get(actor, new HouseholdId(householdId)), actor);
  }

  @PatchMapping("/{householdId}")
  HouseholdResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody UpdateHouseholdRequest request) {
    UserId actor = CurrentUser.of(jwt);
    return HouseholdResponse.from(
        updateHousehold.update(
            actor,
            new HouseholdId(householdId),
            new UpdateHouseholdCommand(request.name(), request.currency(), request.timezone())),
        actor);
  }

  @PatchMapping("/{householdId}/members/{userId}")
  MemberResponse changeRole(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID userId,
      @Valid @RequestBody ChangeRoleRequest request) {
    return MemberResponse.from(
        changeMemberRole.change(
            CurrentUser.of(jwt), new HouseholdId(householdId), new UserId(userId), request.role()));
  }

  @DeleteMapping("/{householdId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void removeMember(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId, @PathVariable UUID userId) {
    removeMember.remove(CurrentUser.of(jwt), new HouseholdId(householdId), new UserId(userId));
  }

  @PutMapping("/{householdId}/owner")
  HouseholdResponse transferOwnership(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody TransferOwnershipRequest request) {
    UserId actor = CurrentUser.of(jwt);
    return HouseholdResponse.from(
        transferOwnership.transfer(
            actor, new HouseholdId(householdId), new UserId(request.userId())),
        actor);
  }
}
