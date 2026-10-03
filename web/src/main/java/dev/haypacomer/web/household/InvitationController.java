package dev.haypacomer.web.household;

import dev.haypacomer.application.household.AcceptInvitation;
import dev.haypacomer.application.household.CancelInvitation;
import dev.haypacomer.application.household.InviteMember;
import dev.haypacomer.application.household.ListInvitations;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Invitation;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.web.household.HouseholdDtos.HouseholdResponse;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvitationController {

  private final InviteMember inviteMember;
  private final ListInvitations listInvitations;
  private final CancelInvitation cancelInvitation;
  private final AcceptInvitation acceptInvitation;

  public InvitationController(
      InviteMember inviteMember,
      ListInvitations listInvitations,
      CancelInvitation cancelInvitation,
      AcceptInvitation acceptInvitation) {
    this.inviteMember = inviteMember;
    this.listInvitations = listInvitations;
    this.cancelInvitation = cancelInvitation;
    this.acceptInvitation = acceptInvitation;
  }

  @PostMapping("/api/v1/households/{householdId}/invitations")
  @ResponseStatus(HttpStatus.CREATED)
  InvitationResponse invite(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody InviteRequest request) {
    return InvitationResponse.from(
        inviteMember.invite(
            CurrentUser.of(jwt), new HouseholdId(householdId), request.email(), request.role()));
  }

  @GetMapping("/api/v1/households/{householdId}/invitations")
  List<InvitationResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return listInvitations.list(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(InvitationResponse::from)
        .toList();
  }

  @DeleteMapping("/api/v1/households/{householdId}/invitations/{invitationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void cancel(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID invitationId) {
    cancelInvitation.cancel(CurrentUser.of(jwt), new HouseholdId(householdId), invitationId);
  }

  @PostMapping("/api/v1/invitations/accept")
  HouseholdResponse accept(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AcceptRequest request) {
    UserId actor = CurrentUser.of(jwt);
    return HouseholdResponse.from(acceptInvitation.accept(actor, request.token()), actor);
  }

  record InviteRequest(@NotBlank @Email String email, @NotNull Role role) {}

  record AcceptRequest(@NotBlank String token) {

    @Override
    public String toString() {
      return "AcceptRequest[protected]";
    }
  }

  record InvitationResponse(UUID id, String email, Role role, Instant expiresAt) {

    static InvitationResponse from(Invitation invitation) {
      return new InvitationResponse(
          invitation.id(), invitation.email().value(), invitation.role(), invitation.expiresAt());
    }
  }
}
